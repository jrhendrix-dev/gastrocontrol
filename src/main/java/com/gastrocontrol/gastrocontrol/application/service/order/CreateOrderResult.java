// src/main/java/com/gastrocontrol/gastrocontrol/service/order/CreateOrderResult.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

import java.util.List;

public class CreateOrderResult {

    private final Long orderId;
    private final OrderType type;
    private final Long tableId;
    private final int totalCents;
    private final OrderStatus status;
    private final DeliverySnapshotDto delivery; // nullable unless DELIVERY
    private final PickupSnapshotDto pickup;     // nullable unless TAKE_AWAY
    private final List<CreateOrderItemResult> items;

    public CreateOrderResult(
            Long orderId,
            OrderType type,
            Long tableId,
            int totalCents,
            OrderStatus status,
            DeliverySnapshotDto delivery,
            PickupSnapshotDto pickup,
            List<CreateOrderItemResult> items
    ) {
        this.orderId = orderId;
        this.type = type;
        this.tableId = tableId;
        this.totalCents = totalCents;
        this.status = status;
        this.delivery = delivery;
        this.pickup = pickup;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public OrderType getType() { return type; }
    public Long getTableId() { return tableId; }
    public int getTotalCents() { return totalCents; }
    public OrderStatus getStatus() { return status; }
    public DeliverySnapshotDto getDelivery() { return delivery; }
    public PickupSnapshotDto getPickup() { return pickup; }
    public List<CreateOrderItemResult> getItems() { return items; }

    public static class CreateOrderItemResult {
        private final Long productId;
        private final String name;
        private final int quantity;
        private final int unitPriceCents;

        public CreateOrderItemResult(Long productId, String name, int quantity, int unitPriceCents) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.unitPriceCents = unitPriceCents;
        }

        public Long getProductId() { return productId; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public int getUnitPriceCents() { return unitPriceCents; }
    }
}
