package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CancelOrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public CancelOrderService(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public OrderResponse handle(Long orderId) {
        OrderJpaEntity order = orderRepository.findHydratedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleViolationException(Map.of(
                    "code", "ORDER_CANCEL_NOT_ALLOWED",
                    "status", String.valueOf(order.getStatus())
            ));
        }

        OrderStatus old = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);

        // Optional: if you want to “free table” hard, keep diningTable but exclude CANCELLED from “active”.
        // Do NOT delete items; keep auditability.
        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_CANCELLED",
                old,
                saved.getStatus(),
                "Order cancelled by STAFF",
                "STAFF",
                null,
                null
        ));

        return StaffOrderMapper.toResponse(saved);
    }
}
