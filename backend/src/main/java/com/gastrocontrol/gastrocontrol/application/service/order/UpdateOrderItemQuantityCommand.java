package com.gastrocontrol.gastrocontrol.application.service.order;

/**
 * Command to update an order item's quantity.
 */
public class UpdateOrderItemQuantityCommand {

    private final Long orderId;
    private final Long itemId;
    private final int quantity;

    public UpdateOrderItemQuantityCommand(Long orderId, Long itemId, int quantity) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Long getOrderId() { return orderId; }
    public Long getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
}