package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.mapper.order.OrderMapper;
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
    public OrderDto handle(ReopenOrderCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "Order id is required"));
        if (command.getReasonCode() == null) throw new ValidationException(Map.of("reasonCode", "Reason code is required"));

        OrderJpaEntity order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        OrderStatus from = order.getStatus();

        if (!Set.of(OrderStatus.READY, OrderStatus.SERVED, OrderStatus.CANCELLED).contains(from)) {
            throw new BusinessRuleViolationException(Map.of(
                    "orderStatus",
                    "Reopen is only allowed from READY, SERVED, or CANCELLED (current: " + from + ")",
                    "orderId",
                    String.valueOf(order.getId())
            ));
        }


        OrderStatus to = OrderStatus.IN_PREPARATION;

        order.setClosedAt(null);
        order.setStatus(to);

        // Persist the change
        OrderJpaEntity saved = orderRepository.save(order);

        // Audit event
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

        // Return hydrated DTO for safe mapping
        OrderJpaEntity hydrated = orderRepository.findHydratedById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Order not found after reopen: " + saved.getId()));

        return OrderMapper.toDto(hydrated);
    }
}
