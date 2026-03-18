// src/main/java/com/gastrocontrol/gastrocontrol/application/service/manager/ManagerForceCancelService.java
package com.gastrocontrol.gastrocontrol.application.service.manager;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Manager-privileged service that force-cancels an order regardless of its current status.
 *
 * <p>Unlike the staff-facing {@code CancelOrderService} which only permits cancellation
 * from DRAFT and PENDING, this service allows a manager to cancel any order that has
 * not already reached a terminal state.</p>
 *
 * <p>Terminal statuses that cannot be force-cancelled:</p>
 * <ul>
 *   <li>{@code FINISHED} — payment already completed; use reopen flow if needed.</li>
 *   <li>{@code CANCELLED} — already cancelled; idempotent no-op would be misleading.</li>
 * </ul>
 *
 * <p>Every force-cancel is recorded in the order event log.</p>
 */
@Service
public class ManagerForceCancelService {

    /** Orders already in these statuses cannot be force-cancelled. */
    private static final Set<OrderStatus> ALREADY_TERMINAL = Set.of(
            OrderStatus.FINISHED,
            OrderStatus.CANCELLED
    );

    private final OrderRepository      orderRepository;
    private final OrderEventRepository orderEventRepository;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for writing the audit trail
     */
    public ManagerForceCancelService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.orderRepository      = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Force-cancels an order, bypassing the staff state-machine restrictions.
     *
     * @param orderId the order to cancel
     * @param message optional audit message explaining the cancellation reason
     * @throws ValidationException            if orderId is null
     * @throws NotFoundException              if the order does not exist
     * @throws BusinessRuleViolationException if the order is already in a terminal state
     */
    @Transactional
    public void handle(Long orderId, String message) {
        if (orderId == null) throw new ValidationException(Map.of("orderId", "Order id is required"));

        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        OrderStatus from = order.getStatus();

        if (ALREADY_TERMINAL.contains(from)) {
            throw new BusinessRuleViolationException(Map.of(
                    "orderStatus",
                    "Order is already " + from + " and cannot be cancelled. "
                            + "Use the reopen flow if you need to modify a finished order."
            ));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setClosedAt(Instant.now());
        orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "MANAGER_FORCE_CANCEL",
                from,
                OrderStatus.CANCELLED,
                message,
                "MANAGER",
                null,
                null
        ));
    }
}