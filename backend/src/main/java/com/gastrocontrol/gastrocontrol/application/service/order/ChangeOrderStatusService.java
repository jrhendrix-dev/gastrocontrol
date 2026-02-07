package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChangeOrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public ChangeOrderStatusService(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public ChangeOrderStatusResult handle(ChangeOrderStatusCommand command) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (command == null) {
            throw new ValidationException(Map.of("command", "Command is required"));
        }
        if (command.getOrderId() == null) errors.put("orderId", "Order id is required");
        if (command.getNewStatus() == null) errors.put("newStatus", "New status is required");

        if (!errors.isEmpty()) throw new ValidationException(errors);

        OrderJpaEntity order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = command.getNewStatus();

        // ✅ Guardrail: customer checkout orders in DRAFT are "awaiting payment"
        // Do NOT allow staff to push them into the kitchen pipeline.
        if (oldStatus == OrderStatus.DRAFT && newStatus == OrderStatus.PENDING) {
            if (order.getType() != OrderType.DINE_IN) {
                throw new BusinessRuleViolationException(Map.of(
                        "newStatus",
                        "Cannot move a customer checkout order to PENDING until payment is confirmed"
                ));
            }
        }

        if (!isValidTransition(oldStatus, newStatus)) {
            Set<OrderStatus> allowed = allowedNextStatuses(oldStatus);

            String allowedText = allowed.isEmpty()
                    ? "[]"
                    : allowed.stream().map(Enum::name).collect(Collectors.joining(", ", "[", "]"));

            throw new BusinessRuleViolationException(Map.of(
                    "newStatus",
                    "Invalid transition: " + oldStatus + " -> " + newStatus + ". Allowed next: " + allowedText
            ));
        }

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
     * Allowed next statuses for a given current status.
     *
     * <p>DRAFT has two meanings:
     * <ul>
     *   <li>DINE_IN: staff POS ticket before submit</li>
     *   <li>TAKE_AWAY/DELIVERY: awaiting payment (customer checkout)</li>
     * </ul>
     * This service includes an extra guardrail to prevent staff from moving customer checkout drafts into PENDING.</p>
     */
    private Set<OrderStatus> allowedNextStatuses(OrderStatus from) {
        return switch (from) {
            case DRAFT -> EnumSet.of(OrderStatus.PENDING, OrderStatus.CANCELLED);

            case PENDING -> EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED);
            case IN_PREPARATION -> EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED);
            case READY -> EnumSet.of(OrderStatus.SERVED);
            case SERVED -> EnumSet.of(OrderStatus.FINISHED);
            case FINISHED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
