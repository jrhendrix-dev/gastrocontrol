// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/CreateOrderRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for creating a new dine-in order.
 */
public class CreateOrderRequest {

    @NotNull(message = "tableId is required")
    private Long tableId;

    @NotEmpty(message = "At least one item is required")
    private List<@Valid OrderItemRequest> items;

    public Long getTableId() { return tableId; }

    public void setTableId(Long tableId) { this.tableId = tableId; }

    public List<OrderItemRequest> getItems() { return items; }

    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    /**
     * One line item in the create-order request.
     */
    public static class OrderItemRequest {

        @NotNull(message = "productId is required")
        private Long productId;

        @Min(value = 1, message = "quantity must be at least 1")
        private int quantity;

        public Long getProductId() { return productId; }

        public void setProductId(Long productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }

        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
