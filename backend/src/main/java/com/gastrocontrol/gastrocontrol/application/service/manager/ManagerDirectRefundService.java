// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerDirectRefundService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Manager-privileged service for issuing a direct (partial or full) refund on a
 * FINISHED order without reopening it.
 *
 * <p>Use this for goodwill gestures, complaints, or billing corrections where
 * the order items do not need to change — only money is returned.</p>
 *
 * <h3>Constraints</h3>
 * <ul>
 *   <li>Order must be in {@code FINISHED} status.</li>
 *   <li>Refund amount must be between 1 cent and the original total (inclusive).</li>
 *   <li>Order must have a SUCCEEDED payment record to refund against.</li>
 * </ul>
 *
 * <p>For MANUAL payments, the refund is recorded as a reference note.
 * For STRIPE, a real gateway call would be needed (currently out of scope).</p>
 */
@Service
public class ManagerDirectRefundService {

    private final OrderRepository      orderRepository;
    private final PaymentRepository    paymentRepository;
    private final OrderEventRepository orderEventRepository;

    public ManagerDirectRefundService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.orderRepository      = orderRepository;
        this.paymentRepository    = paymentRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Result record returned after a successful direct refund.
     *
     * @param orderId         the order that was refunded
     * @param originalTotal   original order total in cents
     * @param refundedCents   the amount refunded in cents
     * @param reason          the reason provided by the manager
     * @param manualReference the reference string for manual tracking
     */
    public record DirectRefundResult(
            Long   orderId,
            int    originalTotal,
            int    refundedCents,
            String reason,
            String manualReference
    ) {}

    /**
     * Issues a direct refund against a finished order.
     *
     * @param orderId         the order to refund
     * @param amountCents     the amount to refund (1 to totalCents, inclusive)
     * @param reason          human-readable reason for the refund
     * @param manualReference optional reference for cash/card tracking
     * @return a result record with the refund details
     * @throws ValidationException            if inputs are invalid
     * @throws NotFoundException              if the order or payment does not exist
     * @throws BusinessRuleViolationException if the order is not FINISHED or has no succeeded payment
     */
    @Transactional
    public DirectRefundResult handle(
            Long orderId,
            int amountCents,
            String reason,
            String manualReference
    ) {
        if (orderId == null) throw new ValidationException(Map.of("orderId", "Order id is required"));
        if (amountCents <= 0) throw new ValidationException(Map.of("amountCents", "Refund amount must be greater than zero"));

        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.FINISHED) {
            throw new BusinessRuleViolationException(Map.of(
                    "orderStatus",
                    "Direct refunds are only allowed on FINISHED orders. Current status: " + order.getStatus()
                            + ". For active orders, use the reopen flow."
            ));
        }

        if (amountCents > order.getTotalCents()) {
            throw new ValidationException(Map.of(
                    "amountCents",
                    "Refund amount (" + amountCents + " cents) cannot exceed order total ("
                            + order.getTotalCents() + " cents)"
            ));
        }

        // Find the succeeded payment to refund against
        PaymentJpaEntity payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new BusinessRuleViolationException(Map.of(
                        "payment", "No payment record found for order " + orderId
                )));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleViolationException(Map.of(
                    "payment",
                    "Cannot refund — payment status is " + payment.getStatus() + ", expected SUCCEEDED"
            ));
        }

        // Record the refund reference on the payment row
        String ref = manualReference != null && !manualReference.isBlank()
                ? manualReference.trim()
                : "Reembolso directo por manager";

        payment.setManualReference(
                (payment.getManualReference() != null ? payment.getManualReference() + " | " : "")
                        + "REFUND: -" + amountCents + " cents — " + ref
        );
        paymentRepository.save(payment);

        // Audit event
        String auditMsg = String.format(
                "Reembolso directo: %d cents. Motivo: %s. Referencia: %s",
                amountCents,
                reason != null ? reason : "Sin motivo especificado",
                ref
        );
        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "MANAGER_DIRECT_REFUND",
                OrderStatus.FINISHED,
                OrderStatus.FINISHED, // status doesn't change
                auditMsg,
                "MANAGER",
                null,
                null
        ));

        return new DirectRefundResult(
                orderId,
                order.getTotalCents(),
                amountCents,
                reason,
                ref
        );
    }
}