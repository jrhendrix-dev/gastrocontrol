// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerOverrideOrderStatusService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Manager-privileged service that overrides an order's status directly,
 * bypassing the normal staff state machine.
 *
 * <p>Intended for corrections only — e.g. an order stuck in the wrong state,
 * a kitchen error that requires rolling back to IN_PREPARATION, etc.
 * Every override is recorded in the order event log for auditability.</p>
 *
 * <p>Constraints that are still enforced even for managers:</p>
 * <ul>
 *   <li>Cannot set the same status the order already has.</li>
 *   <li>Cannot move to {@code DRAFT} — drafts are only created by the POS flow.</li>
 * </ul>
 */
@Service
public class ManagerOverrideOrderStatusService {

    /** Statuses that cannot be set via a manager override. */
    private static final Set<OrderStatus> FORBIDDEN_TARGET_STATUSES = Set.of(OrderStatus.DRAFT);

    private final OrderRepository      orderRepository;
    private final OrderEventRepository orderEventRepository;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for writing the audit trail
     */
    public ManagerOverrideOrderStatusService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.orderRepository      = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Overrides the status of an order without state-machine validation.
     *
     * @param orderId   the order to update
     * @param newStatus the target status
     * @param message   optional audit message explaining the override
     * @throws ValidationException if the target status is invalid or identical to current
     * @throws NotFoundException   if the order does not exist
     */
    @Transactional
    public void handle(Long orderId, OrderStatus newStatus, String message) {
        if (orderId == null)   throw new ValidationException(Map.of("orderId",   "Order id is required"));
        if (newStatus == null) throw new ValidationException(Map.of("newStatus", "New status is required"));

        if (FORBIDDEN_TARGET_STATUSES.contains(newStatus)) {
            throw new ValidationException(Map.of(
                    "newStatus", "Status " + newStatus + " cannot be set via a manager override"
            ));
        }

        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        OrderStatus from = order.getStatus();

        if (from == newStatus) {
            throw new ValidationException(Map.of(
                    "newStatus", "Order is already in status " + newStatus
            ));
        }

        order.setStatus(newStatus);

        // If moving to a terminal status, stamp closedAt; if re-activating, clear it.
        if (newStatus == OrderStatus.FINISHED || newStatus == OrderStatus.CANCELLED) {
            order.setClosedAt(java.time.Instant.now());
        } else {
            order.setClosedAt(null);
        }

        orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "MANAGER_STATUS_OVERRIDE",
                from,
                newStatus,
                message,
                "MANAGER",
                null,
                null
        ));
    }
}