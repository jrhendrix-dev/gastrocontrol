package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
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
 * Use case for adding an item to an existing order.
 *
 * <p>Editability is delegated to {@link OrderEditGuard}, which enforces:</p>
 * <ul>
 *   <li>Status must be DRAFT or PENDING</li>
 *   <li>Payment lock (SUCCEEDED) is bypassed only if the order has been reopened</li>
 * </ul>
 */
@Service
public class AddOrderItemService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final ProductRepository productRepository;
    private final OrderEditGuard orderEditGuard;

    /**
     * @param orderRepository      for loading and persisting the order
     * @param orderEventRepository for the audit trail
     * @param productRepository    for looking up the product to add
     * @param orderEditGuard       shared guard for editability checks
     */
    public AddOrderItemService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            ProductRepository productRepository,
            OrderEditGuard orderEditGuard
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.productRepository = productRepository;
        this.orderEditGuard = orderEditGuard;
    }

    /**
     * Adds a product to the order, merging quantity if the product is already present.
     *
     * @param command must contain orderId, productId, and quantity &gt; 0
     * @return the updated order response
     * @throws ValidationException           if required fields are missing or invalid
     * @throws NotFoundException             if the order or product does not exist
     * @throws com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException
     *                                       if the order is not in an editable state
     */
    @Transactional
    public OrderResponse handle(AddOrderItemCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getOrderId() == null) throw new ValidationException(Map.of("orderId", "orderId is required"));
        if (command.getProductId() == null) throw new ValidationException(Map.of("productId", "productId is required"));
        if (command.getQuantity() <= 0) throw new ValidationException(Map.of("quantity", "quantity must be > 0"));

        OrderJpaEntity order = orderRepository.findHydratedById(command.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + command.getOrderId()));

        // Delegates to shared guard — respects the reopened edit window
        orderEditGuard.assertEditable(order);

        ProductJpaEntity product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + command.getProductId()));

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
}