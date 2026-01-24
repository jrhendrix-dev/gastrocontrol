package com.gastrocontrol.gastrocontrol.application.service.order;

/**
 * Command to add a product to an existing order.
 */
public class AddOrderItemCommand {

    private final Long orderId;
    private final Long productId;
    private final int quantity;

    public AddOrderItemCommand(Long orderId, Long productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}