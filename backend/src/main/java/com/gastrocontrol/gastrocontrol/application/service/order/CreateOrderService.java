// src/main/java/com/gastrocontrol/gastrocontrol/service/order/CreateOrderUseCase.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.DiningTableJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case responsible for creating orders (DINE_IN / TAKE_AWAY / DELIVERY).
 * Validates type-specific inputs and snapshots relevant details into the Order entity.
 */
@Service
public class CreateOrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final DiningTableRepository diningTableRepository;
    private final ProductRepository productRepository;

    public CreateOrderService(
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

        OrderType type = command.getType() == null ? OrderType.DINE_IN : command.getType();

        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new ValidationException(Map.of("items", "At least one item is required"));
        }

        // Reject irrelevant blocks (prevents silent bad requests)
        if (type != OrderType.DELIVERY && command.getDelivery() != null) {
            throw new ValidationException(Map.of("delivery", "Delivery details are only allowed for DELIVERY orders"));
        }
        if (type != OrderType.TAKE_AWAY && command.getPickup() != null) {
            throw new ValidationException(Map.of("pickup", "Pickup details are only allowed for TAKE_AWAY orders"));
        }

        DiningTableJpaEntity table = null;

        if (type == OrderType.DINE_IN) {
            if (command.getTableId() == null) {
                throw new ValidationException(Map.of("tableId", "Table id is required for DINE_IN"));
            }
            table = diningTableRepository.findById(command.getTableId())
                    .orElseThrow(() -> new NotFoundException("Dining table not found: " + command.getTableId()));

            // POS rule: only open tables can be assigned.
            // A table is considered "occupied" if it has any active order.
            var activeStatuses = EnumSet.of(
                    OrderStatus.DRAFT,
                    OrderStatus.PENDING,
                    OrderStatus.IN_PREPARATION,
                    OrderStatus.READY,
                    OrderStatus.SERVED
            );

            if (orderRepository.existsByTypeAndDiningTable_IdAndStatusIn(OrderType.DINE_IN, table.getId(), activeStatuses)) {
                throw new BusinessRuleViolationException(Map.of(
                        "tableId", "Table already has an active order",
                        "message", "Cannot create a new order: table is already occupied"
                ));
            }
        }

        if (type == OrderType.DELIVERY) {
            validateDelivery(command.getDelivery());
        }

        if (type == OrderType.TAKE_AWAY) {
            validatePickup(command.getPickup());
        }

        OrderStatus initial = command.getInitialStatus() == null ? OrderStatus.PENDING : command.getInitialStatus();
        OrderJpaEntity order = new OrderJpaEntity(type, initial, table);


        // Apply delivery snapshot (only for DELIVERY)
        if (type == OrderType.DELIVERY) {
            DeliverySnapshotDto d = command.getDelivery();
            order.setDeliveryName(trimOrNull(d.name()));
            order.setDeliveryPhone(trimOrNull(d.phone()));
            order.setDeliveryAddressLine1(trimOrNull(d.addressLine1()));
            order.setDeliveryAddressLine2(trimOrNull(d.addressLine2()));
            order.setDeliveryCity(trimOrNull(d.city()));
            order.setDeliveryPostalCode(trimOrNull(d.postalCode()));
            order.setDeliveryNotes(trimOrNull(d.notes()));
        } else {
            order.setDeliveryName(null);
            order.setDeliveryPhone(null);
            order.setDeliveryAddressLine1(null);
            order.setDeliveryAddressLine2(null);
            order.setDeliveryCity(null);
            order.setDeliveryPostalCode(null);
            order.setDeliveryNotes(null);
        }

        // Apply pickup snapshot (only for TAKE_AWAY)
        if (type == OrderType.TAKE_AWAY) {
            PickupSnapshotDto p = command.getPickup();
            order.setPickupName(trimOrNull(p.name()));
            order.setPickupPhone(trimOrNull(p.phone()));
            order.setPickupNotes(trimOrNull(p.notes()));
        } else {
            order.setPickupName(null);
            order.setPickupPhone(null);
            order.setPickupNotes(null);
        }

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

            int unitPriceCents = product.getPriceCents();
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

        List<CreateOrderResult.CreateOrderItemResult> resultItems =
                saved.getItems().stream()
                        .map(i -> new CreateOrderResult.CreateOrderItemResult(
                                i.getProduct().getId(),
                                i.getProduct().getName(),
                                i.getQuantity(),
                                i.getUnitPriceCents()
                        ))
                        .collect(Collectors.toList());

        DeliverySnapshotDto resultDelivery = null;
        if (saved.getType() == OrderType.DELIVERY) {
            resultDelivery = new DeliverySnapshotDto(
                    saved.getDeliveryName(),
                    saved.getDeliveryPhone(),
                    saved.getDeliveryAddressLine1(),
                    saved.getDeliveryAddressLine2(),
                    saved.getDeliveryCity(),
                    saved.getDeliveryPostalCode(),
                    saved.getDeliveryNotes()
            );
        }

        PickupSnapshotDto resultPickup = null;
        if (saved.getType() == OrderType.TAKE_AWAY) {
            resultPickup = new PickupSnapshotDto(
                    saved.getPickupName(),
                    saved.getPickupPhone(),
                    saved.getPickupNotes()
            );
        }

        return new CreateOrderResult(
                saved.getId(),
                saved.getType(),
                saved.getDiningTable() == null ? null : saved.getDiningTable().getId(),
                saved.getTotalCents(),
                saved.getStatus(),
                resultDelivery,
                resultPickup,
                resultItems
        );
    }

    private static void validateDelivery(DeliverySnapshotDto d) {
        if (d == null) throw new ValidationException(Map.of("delivery", "Delivery details are required for DELIVERY orders"));

        if (isBlank(d.name())) throw new ValidationException(Map.of("delivery.name", "Name is required"));
        if (isBlank(d.phone())) throw new ValidationException(Map.of("delivery.phone", "Phone is required"));
        if (isBlank(d.addressLine1())) throw new ValidationException(Map.of("delivery.addressLine1", "Address line 1 is required"));
        if (isBlank(d.city())) throw new ValidationException(Map.of("delivery.city", "City is required"));
    }

    private static void validatePickup(PickupSnapshotDto p) {
        if (p == null) throw new ValidationException(Map.of("pickup", "Pickup details are required for TAKE_AWAY orders"));
        if (isBlank(p.name())) throw new ValidationException(Map.of("pickup.name", "Name is required"));
        // phone optional; add later if you want
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}