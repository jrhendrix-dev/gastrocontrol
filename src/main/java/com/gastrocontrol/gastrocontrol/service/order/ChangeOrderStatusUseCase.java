package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        Map<String, String> errors = new HashMap<>();

        if (command.getOrderId() == null) errors.put("orderId", "Order id is required");
        if (command.getNewStatus() == null) errors.put("newStatus", "New status is required");

        if (!errors.isEmpty()) throw new ValidationException(errors);

        OrderJpaEntity order = orderRepository.findById(command.getOrderId()).orElse(null);
        if (order == null) throw new ValidationException(Map.of("orderId", "Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = command.getNewStatus();

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new ValidationException(Map.of(
                    "newStatus",
                    "Invalid transition from " + oldStatus + " to " + newStatus
            ));
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.SERVED || newStatus == OrderStatus.CANCELLED) {
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
                null,  // actorUserId (later from auth/JWT)
                null   // reasonCode (optional for normal transitions)
        ));

        return new ChangeOrderStatusResult(saved.getId(), oldStatus, newStatus);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return true;

        // simple, readable rules (easy to explain in interviews)
        return switch (from) {
            case PENDING -> Set.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED).contains(to);
            case IN_PREPARATION -> Set.of(OrderStatus.READY, OrderStatus.CANCELLED).contains(to);
            case READY -> Set.of(OrderStatus.SERVED).contains(to);
            case SERVED, CANCELLED -> false; // terminal
        };
    }
}
