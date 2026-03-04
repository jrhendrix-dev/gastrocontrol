package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use case for changing the status of an order through the kitchen pipeline.
 *
 * <h3>Transition rules</h3>
 * <pre>
 *   DRAFT → PENDING | CANCELLED
 *   PENDING → IN_PREPARATION | CANCELLED
 *   IN_PREPARATION → READY | CANCELLED
 *   READY → SERVED
 *   SERVED → FINISHED
 *   FINISHED, CANCELLED → (terminal; no further transitions)
 * </pre>
 *
 * <h3>Additional business rules enforced here</h3>
 * <ul>
 *   <li>Customer checkout orders (non-DINE_IN) may not be moved from DRAFT to PENDING
 *       directly — payment confirmation handles that transition.</li>
 *   <li>FINISHED requires payment status SUCCEEDED for all order types.</li>
 *   <li>FINISHED is blocked while {@code order.reopened == true} — the financial
 *       adjustment must be processed first via {@code ProcessOrderAdjustmentService}.</li>
 * </ul>
 */
@Service
public class ChangeOrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for the audit trail
     * @param paymentRepository    for the payment status gate on FINISHED
     */
    public ChangeOrderStatusService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Applies a status transition to the order.
     *
     * @param command must contain orderId and newStatus
     * @return result with old and new statuses
     * @throws ValidationException           if required fields are missing
     * @throws NotFoundException             if the order does not exist
     * @throws BusinessRuleViolationException if any business rule is violated
     */
    @Transactional
    public ChangeOrderStatusResult handle(ChangeOrderStatusCommand command) {
        Map<String, String> validationErrors = new LinkedHashMap<>();

        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) validationErrors.put("orderId", "Order id is required");
        if (command.getNewStatus() == null) validationErrors.put("newStatus", "New status is required");
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        OrderJpaEntity order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = command.getNewStatus();

        // Guardrail: customer checkout orders (TAKE_AWAY / DELIVERY) must not be pushed
        // to PENDING by staff. Payment confirmation handles that transition.
        if (oldStatus == OrderStatus.DRAFT && newStatus == OrderStatus.PENDING) {
            if (order.getType() != OrderType.DINE_IN) {
                throw new BusinessRuleViolationException(Map.of(
                        "newStatus",
                        "Cannot move a customer checkout order to PENDING until payment is confirmed"
                ));
            }
        }

        // Collect ALL rule violations so the caller can fix them in one round-trip.
        Map<String, String> ruleErrors = new LinkedHashMap<>();

        // Rule 1: valid pipeline transition
        if (!isValidTransition(oldStatus, newStatus)) {
            Set<OrderStatus> allowed = allowedNextStatuses(oldStatus);
            String allowedText = allowed.isEmpty()
                    ? "[]"
                    : allowed.stream().map(Enum::name).collect(Collectors.joining(", ", "[", "]"));

            ruleErrors.put(
                    "newStatus",
                    "Invalid transition: " + oldStatus + " -> " + newStatus +
                            ". Allowed next: " + allowedText
            );
        }

        if (newStatus == OrderStatus.FINISHED) {
            // Rule 2: payment must be SUCCEEDED before FINISHED
            PaymentStatus paymentStatus = paymentRepository.findByOrder_Id(order.getId())
                    .map(p -> p.getStatus())
                    .orElse(PaymentStatus.REQUIRES_PAYMENT);

            if (paymentStatus != PaymentStatus.SUCCEEDED) {
                ruleErrors.put(
                        "paymentStatus",
                        "Cannot finish an order until payment is SUCCEEDED " +
                                "(current: " + paymentStatus + ")"
                );
            }

            // Rule 3: reopened orders must process their adjustment before being finished
            if (order.isReopened()) {
                ruleErrors.put(
                        "reopened",
                        "Cannot finish an order while it is in the reopen edit window. " +
                                "Process the financial adjustment first via POST /actions/process-adjustment."
                );
            }
        }

        if (!ruleErrors.isEmpty()) {
            ruleErrors.putIfAbsent("orderId", String.valueOf(order.getId()));
            ruleErrors.putIfAbsent("message", "Business rule violated");
            throw new BusinessRuleViolationException(ruleErrors);
        }

        // Apply the transition
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.FINISHED || newStatus == OrderStatus.CANCELLED) {
            order.setClosedAt(Instant.now());
        }

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "STATUS_CHANGED",
                oldStatus,
                newStatus,
                command.getMessage(),
                "STAFF",
                null,
                null
        ));

        return new ChangeOrderStatusResult(saved.getId(), oldStatus, newStatus);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        return allowedNextStatuses(from).contains(to);
    }

    /**
     * Returns the set of statuses that are valid next steps from the given status.
     *
     * <p>DRAFT has two meanings in this system:</p>
     * <ul>
     *   <li>DINE_IN: a POS draft ticket — can go to PENDING via staff submit action</li>
     *   <li>TAKE_AWAY / DELIVERY: awaiting payment — moved to PENDING by payment confirmation</li>
     * </ul>
     * The extra DINE_IN check is enforced above, not in this method.
     *
     * @param from the current status
     * @return immutable set of allowed next statuses
     */
    private Set<OrderStatus> allowedNextStatuses(OrderStatus from) {
        return switch (from) {
            case DRAFT          -> EnumSet.of(OrderStatus.PENDING, OrderStatus.CANCELLED);
            case PENDING        -> EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED);
            case IN_PREPARATION -> EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED);
            case READY          -> EnumSet.of(OrderStatus.SERVED);
            case SERVED         -> EnumSet.of(OrderStatus.FINISHED);
            case FINISHED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}