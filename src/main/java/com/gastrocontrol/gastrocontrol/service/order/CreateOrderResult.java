package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;

import java.util.List;

public class CreateOrderResult {

    private final Long orderId;
    private final Long tableId;
    private final int totalCents;
    private final OrderStatus status;
    private final List<CreateOrderItemResult> items;

    public CreateOrderResult(Long orderId, Long tableId, int totalCents, OrderStatus status, List<CreateOrderItemResult> items) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.totalCents = totalCents;
        this.status = status;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public Long getTableId() { return tableId; }
    public int getTotalCents() { return totalCents; }
    public OrderStatus getStatus() { return status; }
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
