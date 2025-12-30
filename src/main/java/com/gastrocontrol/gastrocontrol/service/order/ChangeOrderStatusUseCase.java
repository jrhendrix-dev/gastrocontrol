package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChangeOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public ChangeOrderStatusUseCase(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
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

        if (newStatus == OrderStatus.SERVED || newStatus == OrderStatus.CANCELLED) {
            order.setClosedAt(Instant.now());
        } else {
            // optional: if you want reopening READY -> etc. to always ensure closedAt is null
            // order.setClosedAt(null);
        }

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "STATUS_CHANGED",
                oldStatus,
                newStatus,
                command.getMessage(),
                "STAFF",
                null,  // actorUserId later from auth/JWT
                null   // reasonCode (optional for normal transitions)
        ));

        return new ChangeOrderStatusResult(saved.getId(), oldStatus, newStatus);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        return allowedNextStatuses(from).contains(to);
    }

    private Set<OrderStatus> allowedNextStatuses(OrderStatus from) {
        return switch (from) {
            case PENDING -> EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED);
            case IN_PREPARATION -> EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED);
            case READY -> EnumSet.of(OrderStatus.SERVED);
            case SERVED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
