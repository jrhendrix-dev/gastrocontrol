// src/main/java/com/gastrocontrol/gastrocontrol/service/order/CreateOrderCommand.java
package com.gastrocontrol.gastrocontrol.service.order;

import java.util.List;
import java.util.Objects;

/**
 * Command object used to request creation of a new dine-in order.
 */
public class CreateOrderCommand {

    private final Long tableId;
    private final List<CreateOrderItem> items;

    public CreateOrderCommand(Long tableId, List<CreateOrderItem> items) {
        this.tableId = Objects.requireNonNull(tableId, "tableId must not be null");
        this.items = Objects.requireNonNull(items, "items must not be null");
    }

    public Long getTableId() { return tableId; }

    public List<CreateOrderItem> getItems() { return items; }

    /**
     * Single line item within the create-order command.
     */
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
