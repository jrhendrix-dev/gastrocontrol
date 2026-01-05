// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/OrderResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;

import java.util.List;

/**
 * Response payload for a created order (staff-facing).
 * <p>
 * Always includes {@link OrderType}. Delivery / pickup snapshots are included
 * only when relevant to the order type.
 */
public class OrderResponse {

    private Long id;
    private OrderType type;

    /** Present only for DINE_IN orders */
    private Long tableId;

    private int totalCents;
    private OrderStatus status;

    /** Present only for DELIVERY orders */
    private DeliverySnapshotDto delivery;

    /** Present only for TAKE_AWAY orders */
    private PickupSnapshotDto pickup;

    private List<OrderItemResponse> items;

    // ---------- Getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public int getTotalCents() { return totalCents; }
    public void setTotalCents(int totalCents) { this.totalCents = totalCents; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public DeliverySnapshotDto getDelivery() { return delivery; }
    public void setDelivery(DeliverySnapshotDto delivery) { this.delivery = delivery; }

    public PickupSnapshotDto getPickup() { return pickup; }
    public void setPickup(PickupSnapshotDto pickup) { this.pickup = pickup; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    // ---------- Nested DTO ----------

    public static class OrderItemResponse {
        private Long productId;
        private String name;
        private int quantity;
        private int unitPriceCents;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public int getUnitPriceCents() { return unitPriceCents; }
        public void setUnitPriceCents(int unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    }
}
