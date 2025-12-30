package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final DiningTableRepository diningTableRepository;
    private final ProductRepository productRepository;

    public CreateOrderUseCase(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            DiningTableRepository diningTableRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.diningTableRepository = diningTableRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CreateOrderResult handle(CreateOrderCommand command) {
        if (command == null) throw new ValidationException(Map.of("command", "Command is required"));
        if (command.getTableId() == null) throw new ValidationException(Map.of("tableId", "Table id is required"));
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new ValidationException(Map.of("items", "At least one item is required"));
        }

        DiningTableJpaEntity table = diningTableRepository.findById(command.getTableId())
                .orElseThrow(() -> new NotFoundException("Dining table not found: " + command.getTableId()));

        OrderJpaEntity order = new OrderJpaEntity(OrderType.DINE_IN, OrderStatus.PENDING, table);

        // Build items + compute total
        int totalCents = 0;

        for (CreateOrderCommand.CreateOrderItem item : command.getItems()) {
            if (item.getProductId() == null) {
                throw new ValidationException(Map.of("productId", "Product id is required"));
            }
            if (item.getQuantity() <= 0) {
                throw new ValidationException(Map.of("quantity", "Quantity must be > 0"));
            }

            ProductJpaEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + item.getProductId()));

            int unitPriceCents = product.getPriceCents(); // assumes ProductJpaEntity has getPriceCents()
            int lineTotal = unitPriceCents * item.getQuantity();
            totalCents += lineTotal;

            OrderItemJpaEntity oi = new OrderItemJpaEntity(product, item.getQuantity(), unitPriceCents);
            order.addItem(oi);
        }

        order.setTotalCents(totalCents);

        OrderJpaEntity saved = orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                saved,
                "ORDER_CREATED",
                null,
                saved.getStatus(),
                "Order created",
                "STAFF",
                null,
                null
        ));

        // Build result (includes item info)
        List<CreateOrderResult.CreateOrderItemResult> resultItems =
                saved.getItems().stream()
                        .map(i -> new CreateOrderResult.CreateOrderItemResult(
                                i.getProduct().getId(),
                                i.getProduct().getName(),
                                i.getQuantity(),
                                i.getUnitPriceCents()
                        ))
                        .collect(Collectors.toList());

        return new CreateOrderResult(
                saved.getId(),
                saved.getDiningTable().getId(),
                saved.getTotalCents(),
                saved.getStatus(),
                resultItems
        );
    }
}
