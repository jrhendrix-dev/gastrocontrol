package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Shared guard that enforces whether an order's items may be modified.
 *
 * <p>Extracted here to avoid duplicating the same logic across
 * {@code AddOrderItemService}, {@code RemoveOrderItemService}, and
 * {@code UpdateOrderItemQuantityService}.</p>
 *
 * <h3>Edit rules</h3>
 * <ol>
 *   <li>Order status must be {@code DRAFT} or {@code PENDING}.</li>
 *   <li>If payment is {@code SUCCEEDED}, edits are blocked — unless the order
 *       has been explicitly reopened ({@code order.isReopened() == true}),
 *       which grants a temporary edit window managed by
 *       {@code ReopenOrderService} and {@code ProcessOrderAdjustmentService}.</li>
 * </ol>
 */
@Component
public class OrderEditGuard {

    private final PaymentRepository paymentRepository;

    /**
     * @param paymentRepository used to look up the current payment status
     */
    public OrderEditGuard(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Asserts that the given order may have its items modified.
     *
     * @param order the order to check
     * @throws BusinessRuleViolationException if the order is not in an editable state
     */
    public void assertEditable(OrderJpaEntity order) {
        // Rule 1: status gate
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleViolationException(Map.of(
                    "status",
                    "Order items can only be modified while status is DRAFT or PENDING " +
                            "(current: " + order.getStatus() + ")"
            ));
        }

        // Rule 2: reopened flag bypasses the payment lock entirely.
        // The manager has explicitly unlocked this order for editing via ReopenOrderService.
        if (order.isReopened()) {
            return;
        }

        // Rule 3: payment lock for non-reopened orders
        PaymentStatus ps = paymentRepository.findByOrder_Id(order.getId())
                .map(p -> p.getStatus())   // 'p' — not shadowing the class name
                .orElse(PaymentStatus.REQUIRES_PAYMENT);

        if (ps == PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleViolationException(Map.of(
                    "paymentStatus",
                    "Order is locked because payment is SUCCEEDED. " +
                            "Use the reopen endpoint to unlock editing.",
                    "orderId",
                    String.valueOf(order.getId())
            ));
        }
    }
}