// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerReopenOrderService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.application.service.order.ReopenOrderCommand;
import com.gastrocontrol.gastrocontrol.application.service.order.ReopenOrderService;
import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderEventReasonCode;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.DiningTableJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manager-privileged reopen service with additional business rules:
 *
 * <ol>
 *   <li><strong>30-minute window</strong> — an order can only be reopened within
 *       30 minutes of being FINISHED or CANCELLED. After that window, the order
 *       is considered closed for financial integrity purposes.</li>
 *   <li><strong>Table conflict resolution</strong> — if the order's original table
 *       is now occupied by a different active order, the table association is cleared
 *       before reopening, converting it to a "detached" order that the manager
 *       handles manually.</li>
 * </ol>
 *
 * <p>After applying these checks this service delegates to the existing
 * {@link ReopenOrderService} which handles the core reopen logic, payment
 * snapshot, and audit trail.</p>
 */
@Service
public class ManagerReopenOrderService {

    /** Maximum time after FINISHED/CANCELLED that a manager can reopen an order. */
    private static final Duration REOPEN_WINDOW = Duration.ofMinutes(30);

    /** Statuses considered "active" — used to detect table conflicts. */
    private static final Set<OrderStatus> ACTIVE_STATUSES = Set.of(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.IN_PREPARATION,
            OrderStatus.READY,
            OrderStatus.SERVED
    );

    private final OrderRepository   orderRepository;
    private final ReopenOrderService reopenOrderService;

    /**
     * @param orderRepository    for loading the order and checking table conflicts
     * @param reopenOrderService the existing core reopen service to delegate to
     */
    public ManagerReopenOrderService(
            OrderRepository orderRepository,
            ReopenOrderService reopenOrderService
    ) {
        this.orderRepository   = orderRepository;
        this.reopenOrderService = reopenOrderService;
    }

    /**
     * Reopens an order with manager-level validations applied.
     *
     * <p>If the order's original table is now occupied by another active order,
     * the table association is detached <em>before</em> the reopen so the new
     * table occupant is not affected.</p>
     *
     * @param orderId    the order to reopen
     * @param reasonCode the reason for reopening (required)
     * @param message    optional audit message
     * @return the updated order DTO after reopening
     * @throws ValidationException            if orderId or reasonCode is missing
     * @throws NotFoundException              if the order does not exist
     * @throws BusinessRuleViolationException if outside the 30-minute reopen window
     */
    @Transactional
    public OrderDto handle(Long orderId, OrderEventReasonCode reasonCode, String message) {
        if (orderId == null)    throw new ValidationException(Map.of("orderId",    "Order id is required"));
        if (reasonCode == null) throw new ValidationException(Map.of("reasonCode", "Reason code is required"));

        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // ── Rule 1: 30-minute reopen window ───────────────────────────────────
        // We measure from updatedAt (the last time the order was touched),
        // which is stamped by @PreUpdate on every save. This is the safest
        // proxy for "when it was finished/cancelled" without a dedicated closedAt
        // index.
        Instant lastUpdated = order.getUpdatedAt() != null
                ? order.getUpdatedAt()
                : order.getCreatedAt();

        Duration elapsed = Duration.between(lastUpdated, Instant.now());
        if (elapsed.compareTo(REOPEN_WINDOW) > 0) {
            long minutesAgo = elapsed.toMinutes();
            throw new BusinessRuleViolationException(Map.of(
                    "reopenWindow",
                    "Order can only be reopened within 30 minutes of closing. "
                            + "This order was last updated " + minutesAgo + " minutes ago."
            ));
        }

        // ── Rule 2: Table conflict detection + detach ─────────────────────────
        DiningTableJpaEntity originalTable = order.getDiningTable();
        if (originalTable != null) {
            boolean tableOccupied = hasActiveOrderOnTable(originalTable.getId(), orderId);
            if (tableOccupied) {
                // Detach the table before reopening to avoid conflicting with the new order.
                // The manager will need to handle this order as a walk-up/detached ticket.
                order.setDiningTable(null);
                orderRepository.save(order);
            }
        }

        // ── Delegate to existing reopen service ───────────────────────────────
        return reopenOrderService.handle(
                new ReopenOrderCommand(orderId, reasonCode, message)
        );
    }

    /**
     * Returns true if there is any active (non-terminal, non-cancelled) order
     * on the given table that is NOT the order being reopened.
     *
     * @param tableId       the table to check
     * @param excludeOrderId the order being reopened (excluded from the check)
     */
    private boolean hasActiveOrderOnTable(Long tableId, Long excludeOrderId) {
        List<OrderJpaEntity> tableOrders = orderRepository.findByDiningTable_Id(tableId);
        return tableOrders.stream()
                .filter(o -> !o.getId().equals(excludeOrderId))
                .anyMatch(o -> ACTIVE_STATUSES.contains(o.getStatus()));
    }
}