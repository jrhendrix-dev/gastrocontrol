// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/OrderResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;

import java.util.List;

/**
 * Response payload for a created order.
 */
public class OrderResponse {

    private Long id;
    private Long tableId;
    private int totalCents;
    private OrderStatus status;
    private List<OrderItemResponse> items;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTableId() { return tableId; }

    public void setTableId(Long tableId) { this.tableId = tableId; }

    public int getTotalCents() { return totalCents; }

    public void setTotalCents(int totalCents) { this.totalCents = totalCents; }

    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItemResponse> getItems() { return items; }

    public void setItems(List<OrderItemResponse> items) { this.items = items; }

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
