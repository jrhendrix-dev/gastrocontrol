// src/main/java/com/gastrocontrol/gastrocontrol/service/order/CreateOrderCommand.java
package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;

import java.util.List;
import java.util.Objects;

/**
 * Command object used to request creation of a new order.
 */
public class CreateOrderCommand {

    private final OrderType type;                 // DINE_IN / TAKE_AWAY / DELIVERY
    private final Long tableId;                   // required if DINE_IN
    private final DeliverySnapshotDto delivery;   // required if DELIVERY
    private final List<CreateOrderItem> items;

    public CreateOrderCommand(
            OrderType type,
            Long tableId,
            DeliverySnapshotDto delivery,
            List<CreateOrderItem> items
    ) {
        this.type = type == null ? OrderType.DINE_IN : type;
        this.tableId = tableId;
        this.delivery = delivery;
        this.items = Objects.requireNonNull(items, "items must not be null");
    }

    public OrderType getType() { return type; }
    public Long getTableId() { return tableId; }
    public DeliverySnapshotDto getDelivery() { return delivery; }
    public List<CreateOrderItem> getItems() { return items; }

    public static class CreateOrderItem {
        private final Long productId;
        private final int quantity;

        public CreateOrderItem(Long productId, int quantity) {
            this.productId = Objects.requireNonNull(productId, "productId must not be null");
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
    }
}
