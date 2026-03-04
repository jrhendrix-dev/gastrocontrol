package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.mapper.order.OrderMapper;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Use case for reopening a completed or ready order to allow item modifications.
 *
 * <h3>What this service does</h3>
 * <ol>
 *   <li>Validates the command and checks the order exists.</li>
 *   <li>Ensures the order is in a reopenable status (READY, SERVED, FINISHED, CANCELLED).</li>
 *   <li>Moves the order to {@code PENDING} — back into the kitchen queue — so staff
 *       can see the updated ticket before preparing it again.</li>
 *   <li>Sets {@code order.reopened = true} to grant the edit window for item modifications,
 *       even if payment is already SUCCEEDED.</li>
 *   <li>If the order has a SUCCEEDED payment, snapshots {@code paidAmountCents} on the
 *       payment record so {@code ProcessOrderAdjustmentService} can compute the delta.</li>
 *   <li>Clears {@code closedAt} since the order is active again.</li>
 *   <li>Records an audit event.</li>
 * </ol>
 *
 * <h3>What this service does NOT do</h3>
 * <ul>
 *   <li>It does NOT issue refunds or extra charges — that is handled separately by
 *       {@code ProcessOrderAdjustmentService} after the manager has finished modifying items.</li>
 * </ul>
 *
 * @see ProcessOrderAdjustmentService
 */
@Service
public class ReopenOrderService {

    private static final Set<OrderStatus> REOPENABLE_STATUSES = Set.of(
            OrderStatus.READY,
            OrderStatus.SERVED,
            OrderStatus.FINISHED,
            OrderStatus.CANCELLED
    );

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for writing the audit trail
     * @param paymentRepository    for snapshotting {@code paidAmountCents}
     */
    public ReopenOrderService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Reopens an order, moving it back to {@code PENDING} and enabling item edits.
     *
     * @param command the reopen request; must include orderId and reasonCode
     * @return a DTO of the updated order
     * @throws ValidationException           if required fields are missing
     * @throws NotFoundException             if the order does not exist
     * @throws BusinessRuleViolationException if the order is not in a reopenable status
     */
    @Transactional
    public OrderDto handle(ReopenOrderCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "Order id is required"));
        if (command.getReasonCode() == null) throw new ValidationException(Map.of("reasonCode", "Reason code is required"));

        OrderJpaEntity order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        OrderStatus from = order.getStatus();

        if (!REOPENABLE_STATUSES.contains(from)) {
            throw new BusinessRuleViolationException(Map.of(
                    "orderStatus",
                    "Reopen is only allowed from: " + REOPENABLE_STATUSES +
                            " (current: " + from + ")",
                    "orderId",
                    String.valueOf(order.getId())
            ));
        }

        // Snapshot paidAmountCents BEFORE modifying anything, so the adjustment service
        // can accurately compute the delta. We only do this if payment is SUCCEEDED.
        paymentRepository.findByOrder_Id(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCEEDED
                    && payment.getPaidAmountCents() == null) {
                // Backfill: set to amountCents as a safe fallback if not already captured.
                // Going forward, HandleStripeCheckoutWebhookService and ConfirmManualPaymentService
                // will set this at confirmation time.
                payment.setPaidAmountCents(payment.getAmountCents());
                paymentRepository.save(payment);
            }
        });

        OrderStatus to = OrderStatus.PENDING;

        order.setStatus(to);
        order.setClosedAt(null);
        order.setReopened(true);  // Grants edit window despite SUCCEEDED payment

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_REOPENED",
                from,
                to,
                command.getMessage(),
                "MANAGER",
                null,
                command.getReasonCode()
        ));

        // Return hydrated DTO for safe mapping (avoids lazy-load issues)
        OrderJpaEntity hydrated = orderRepository.findHydratedById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Order not found after reopen: " + saved.getId()));

        return OrderMapper.toDto(hydrated);
    }
}