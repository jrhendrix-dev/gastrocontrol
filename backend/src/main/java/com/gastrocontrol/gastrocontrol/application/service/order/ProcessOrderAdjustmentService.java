package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.application.port.payment.*;
import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Use case for processing the financial adjustment after a reopened order has been modified.
 *
 * <h3>Preconditions</h3>
 * <ul>
 *   <li>The order must exist and have {@code reopened = true}.</li>
 *   <li>The order must have an existing payment record with a known {@code paidAmountCents}.</li>
 * </ul>
 *
 * <h3>Delta logic</h3>
 * <pre>
 *   delta = newTotalCents - paidAmountCents
 *
 *   delta &gt; 0 → extra charge (STUB: Stripe | MANUAL: manager records reference)
 *   delta &lt; 0 → partial refund (STUB: Stripe | MANUAL: manager records reference)
 *   delta == 0 → no financial action needed
 * </pre>
 *
 * <h3>Post-conditions</h3>
 * <ul>
 *   <li>{@code order.reopened} is set back to {@code false}, closing the edit window.</li>
 *   <li>The payment's {@code amountCents} is updated to the new total.</li>
 *   <li>An audit event is written.</li>
 * </ul>
 *
 * @see ReopenOrderService
 */
@Service
public class ProcessOrderAdjustmentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentGateway paymentGateway;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param paymentRepository    for loading and updating the payment record
     * @param orderEventRepository for the audit trail
     * @param paymentGateway       port for gateway-specific refund / charge operations
     */
    public ProcessOrderAdjustmentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository,
            PaymentGateway paymentGateway
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Processes the financial adjustment for a reopened and modified order.
     *
     * @param command must contain orderId and provider; manualReference required for MANUAL provider
     * @return result describing the action taken and the delta
     * @throws ValidationException           if required fields are missing or invalid
     * @throws NotFoundException             if the order or payment does not exist
     * @throws BusinessRuleViolationException if the order is not in the edit window
     */
    @Transactional
    public ProcessOrderAdjustmentResult handle(ProcessOrderAdjustmentCommand command) {
        // --- Input validation ---
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.orderId() == null) throw new ValidationException(Map.of("orderId", "orderId is required"));
        if (command.provider() == null) throw new ValidationException(Map.of("provider", "provider is required"));

        if (command.provider() == PaymentProvider.MANUAL) {
            if (command.manualReference() == null || command.manualReference().isBlank()) {
                throw new ValidationException(Map.of(
                        "manualReference",
                        "manualReference is required for MANUAL provider adjustments"
                ));
            }
        }

        // --- Load order ---
        OrderJpaEntity order = orderRepository.findHydratedById(command.orderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.orderId()));

        if (!order.isReopened()) {
            throw new BusinessRuleViolationException(Map.of(
                    "reopened",
                    "Order is not in the reopen edit window. Only reopened orders can be adjusted.",
                    "orderId",
                    String.valueOf(order.getId())
            ));
        }

        // --- Load payment ---
        PaymentJpaEntity payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Payment not found for order: " + order.getId() +
                                ". Cannot compute adjustment delta."
                ));

        Integer paidAmountCents = payment.getPaidAmountCents();
        if (paidAmountCents == null) {
            throw new BusinessRuleViolationException(Map.of(
                    "paidAmountCents",
                    "Payment has no paidAmountCents snapshot. " +
                            "This can happen if the order was reopened before the snapshot backfill. " +
                            "Contact support or confirm payment first."
            ));
        }

        int newTotalCents = order.getTotalCents();
        int deltaCents = newTotalCents - paidAmountCents;

        String adjustmentType;
        String providerReference = null;

        // --- Apply financial action ---
        if (deltaCents == 0) {
            adjustmentType = "NO_ADJUSTMENT";

        } else if (deltaCents < 0) {
            // New total is LESS than what was paid → refund the difference
            int refundAmount = Math.abs(deltaCents);
            adjustmentType = "REFUND";

            if (command.provider() == PaymentProvider.STRIPE) {
                // Gateway call — currently stubbed; throws UnsupportedOperationException
                // until StripePaymentGateway#issueRefund is implemented.
                String idempotencyKey = "refund-order-" + order.getId() + "-" + Instant.now().toEpochMilli();
                IssueRefundResult refundResult = paymentGateway.issueRefund(new IssueRefundCommand(
                        payment.getPaymentIntentId(),
                        refundAmount,
                        "Order adjustment after reopen (orderId=" + order.getId() + ")",
                        idempotencyKey
                ));
                providerReference = refundResult.refundId();

            } else {
                // MANUAL: manager records the refund reference (e.g., cash given back, terminal refund id)
                providerReference = command.manualReference();
            }

        } else {
            // deltaCents > 0 — New total is MORE than what was paid → charge the difference
            adjustmentType = "EXTRA_CHARGE";

            if (command.provider() == PaymentProvider.STRIPE) {
                // Gateway call — currently stubbed; throws UnsupportedOperationException
                // until StripePaymentGateway#startAdjustmentCharge is implemented.
                String idempotencyKey = "adj-order-" + order.getId() + "-" + Instant.now().toEpochMilli();
                StartAdjustmentChargeResult chargeResult = paymentGateway.startAdjustmentCharge(
                        new StartAdjustmentChargeCommand(
                                order.getId(),
                                deltaCents,
                                payment.getCurrency(),
                                "Order adjustment for orderId=" + order.getId(),
                                idempotencyKey,
                                Map.of("orderId", String.valueOf(order.getId()))
                        )
                );
                providerReference = chargeResult.paymentIntentId();

            } else {
                // MANUAL: manager records the extra charge reference (e.g., terminal receipt)
                providerReference = command.manualReference();
            }
        }

        // --- Update payment record to reflect new total ---
        payment.setAmountCents(newTotalCents);
        // paidAmountCents stays as-is (it's the historical snapshot of what was originally paid)
        paymentRepository.save(payment);

        // --- Close the edit window ---
        order.setReopened(false);
        orderRepository.save(order);

        // --- Audit event ---
        String auditMessage = String.format(
                "Adjustment processed: type=%s, paidCents=%d, newTotalCents=%d, deltaCents=%d, ref=%s",
                adjustmentType, paidAmountCents, newTotalCents, deltaCents, providerReference
        );

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "ORDER_ADJUSTMENT_PROCESSED",
                order.getStatus(),
                order.getStatus(),
                auditMessage,
                "MANAGER",
                null,
                null
        ));

        return new ProcessOrderAdjustmentResult(
                order.getId(),
                paidAmountCents,
                newTotalCents,
                deltaCents,
                adjustmentType,
                providerReference
        );
    }
}