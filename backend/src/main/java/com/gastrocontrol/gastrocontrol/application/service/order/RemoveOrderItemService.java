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
 * Removes an item from an order.
 */
@Service
public class RemoveOrderItemService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public RemoveOrderItemService(OrderRepository orderRepository, OrderEventRepository orderEventRepository) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public OrderResponse handle(RemoveOrderItemCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "orderId is required"));
        if (command.getItemId() == null) throw new ValidationException(Map.of("itemId", "itemId is required"));

        OrderJpaEntity order = orderRepository.findHydratedById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        assertEditable(order);

        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(command.getItemId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            throw new NotFoundException(
                    "Order item not found: orderId=" + command.getOrderId() + ", itemId=" + command.getItemId()
            );
        }

        order.removeItem(item);

        order.setTotalCents(OrderTotalCalculator.computeTotalCents(order.getItems()));
        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_ITEM_REMOVED",
                saved.getStatus(),
                saved.getStatus(),
                "Removed itemId=" + command.getItemId(),
                "STAFF",
                null,
                null
        ));

        return StaffOrderMapper.toResponse(saved);
    }

    private static void assertEditable(OrderJpaEntity order) {
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleViolationException(Map.of(
                    "status",
                    "Order items can only be modified while status is DRAFT or PENDING"
            ));
        }
    }
}