package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
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

/**
 * Submits a draft ticket to the kitchen (DRAFT -> PENDING).
 */
@Service
public class SubmitOrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public SubmitOrderService(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public OrderResponse handle(Long orderId) {
        if (orderId == null) throw new ValidationException(Map.of("orderId", "orderId is required"));

        OrderJpaEntity order = orderRepository.findHydratedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PENDING) {
            // idempotent: already submitted
            return StaffOrderMapper.toResponse(order);
        }

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException(Map.of(
                    "status",
                    "Only DRAFT orders can be submitted (current=" + order.getStatus() + ")"
            ));
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessRuleViolationException(Map.of(
                    "items",
                    "Cannot submit an empty ticket. Add at least one item first."
            ));
        }

        OrderStatus from = order.getStatus();
        order.setStatus(OrderStatus.PENDING);

        // total should already be accurate, but make it safe
        order.setTotalCents(OrderTotalCalculator.computeTotalCents(order.getItems()));

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_SUBMITTED",
                from,
                saved.getStatus(),
                "Submitted to kitchen",
                "STAFF",
                null,
                null
        ));

        return StaffOrderMapper.toResponse(saved);
    }
}