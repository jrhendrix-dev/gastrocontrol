package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class ReopenOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public ReopenOrderUseCase(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public void handle(ReopenOrderCommand command) {
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "Order id is required"));
        if (command.getReasonCode() == null) throw new ValidationException(Map.of("reasonCode", "Reason code is required"));

        OrderJpaEntity order = orderRepository.findById(command.getOrderId()).orElse(null);
        if (order == null) throw new ValidationException(Map.of("orderId", "Order not found"));

        OrderStatus from = order.getStatus();

        if (!Set.of(OrderStatus.READY, OrderStatus.SERVED, OrderStatus.CANCELLED).contains(from)) {
            throw new ValidationException(Map.of(
                    "orderStatus", "Reopen is only allowed from READY, SERVED, or CANCELLED (current: " + from + ")"
            ));
        }

        OrderStatus to = OrderStatus.IN_PREPARATION;

        order.setClosedAt(null);
        order.setStatus(to);

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_REOPENED",
                from,
                to,
                command.getMessage(),
                "MANAGER",
                null,                 // actorUserId later from JWT
                command.getReasonCode()
        ));
    }
}
