package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import com.gastrocontrol.gastrocontrol.mapper.order.StaffOrderMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Adds an item to an order (or increments quantity if the product is already present).
 */
@Service
public class AddOrderItemService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final ProductRepository productRepository;

    public AddOrderItemService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse handle(AddOrderItemCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "orderId is required"));
        if (command.getProductId() == null) throw new ValidationException(Map.of("productId", "productId is required"));
        if (command.getQuantity() <= 0) throw new ValidationException(Map.of("quantity", "quantity must be > 0"));

        OrderJpaEntity order = orderRepository.findHydratedById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        assertEditable(order);

        ProductJpaEntity product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + command.getProductId()));

        // Unique constraint (order_id, product_id) exists, so "add" should merge.
        OrderItemJpaEntity existing = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + command.getQuantity());
        } else {
            OrderItemJpaEntity oi = new OrderItemJpaEntity(product, command.getQuantity(), product.getPriceCents());
            order.addItem(oi);
        }

        order.setTotalCents(OrderTotalCalculator.computeTotalCents(order.getItems()));
        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_ITEM_ADDED",
                saved.getStatus(),
                saved.getStatus(),
                "Added productId=" + product.getId() + ", qty=" + command.getQuantity(),
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