package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.entity.*;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;
import com.gastrocontrol.gastrocontrol.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreateOrderUseCase {

    private static final EnumSet<OrderStatus> OPEN_STATUSES =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION, OrderStatus.READY);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;
    private final OrderEventRepository orderEventRepository;


    public CreateOrderUseCase(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            DiningTableRepository tableRepository,
            OrderEventRepository orderEventRepository

    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tableRepository = tableRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public CreateOrderResult handle(CreateOrderCommand command) {
        Map<String, String> errors = new HashMap<>();

        // 1) Validate table
        DiningTableJpaEntity table = tableRepository.findById(command.getTableId()).orElse(null);
        if (table == null) {
            errors.put("tableId", "Table does not exist");
        } else {
            boolean hasOpenOrder = orderRepository
                    .findFirstByTypeAndDiningTable_IdAndStatusInOrderByCreatedAtDesc(
                            OrderType.DINE_IN,
                            table.getId(),
                            OPEN_STATUSES
                    )
                    .isPresent();

            if (hasOpenOrder) {
                errors.put("tableId", "Table already has an open order");
            }
        }

        // 2) Validate items
        List<CreateOrderCommand.CreateOrderItem> items = command.getItems();
        if (items == null || items.isEmpty()) {
            errors.put("items", "At least one item is required");
        }

        // We'll build these only for valid rows
        List<OrderItemJpaEntity> entityItems = new ArrayList<>();
        List<CreateOrderResult.CreatedOrderItem> resultItems = new ArrayList<>();
        int totalCents = 0;

        if (items != null) {
            for (int index = 0; index < items.size(); index++) {
                CreateOrderCommand.CreateOrderItem itemCmd = items.get(index);
                String prefix = "items[" + index + "]";

                if (itemCmd.getQuantity() <= 0) {
                    errors.put(prefix + ".quantity", "Quantity must be greater than zero");
                }

                ProductJpaEntity product = productRepository.findById(itemCmd.getProductId()).orElse(null);
                if (product == null) {
                    errors.put(prefix + ".productId", "Product does not exist");
                } else if (!product.isActive()) {
                    errors.put(prefix + ".productId", "Product is not available");
                }

                // Add only valid items
                if (product != null && product.isActive() && itemCmd.getQuantity() > 0) {
                    int unitPrice = product.getPriceCents();
                    int lineTotal = unitPrice * itemCmd.getQuantity();

                    totalCents += lineTotal;

                    entityItems.add(new OrderItemJpaEntity(product, itemCmd.getQuantity(), unitPrice));

                    resultItems.add(new CreateOrderResult.CreatedOrderItem(
                            product.getId(),
                            product.getName(),
                            itemCmd.getQuantity(),
                            unitPrice
                    ));
                }
            }
        }

        // 3) Stop early if invalid
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 4) Build + persist order entity
        OrderJpaEntity order = new OrderJpaEntity();
        order.setType(OrderType.DINE_IN);
        order.setStatus(OrderStatus.PENDING);
        order.setDiningTable(table);
        // createdAt will be set by @PrePersist in OrderJpaEntity

        for (OrderItemJpaEntity item : entityItems) {
            order.addItem(item); // sets item.order too
        }

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_CREATED",
                null,
                saved.getStatus(),
                "Order created",
                "STAFF"
        ));


        assert table != null;
        return new CreateOrderResult(
                saved.getId(),
                table.getId(),
                totalCents,
                saved.getStatus(),
                resultItems
        );
    }
}
