package com.gastrocontrol.gastrocontrol.application.service.order;

/**
 * Command to remove an item from an order.
 */
public class RemoveOrderItemCommand {

    private final Long orderId;
    private final Long itemId;

    public RemoveOrderItemCommand(Long orderId, Long itemId) {
        this.orderId = orderId;
        this.itemId = itemId;
    }

    public Long getOrderId() { return orderId; }
    public Long getItemId() { return itemId; }
}