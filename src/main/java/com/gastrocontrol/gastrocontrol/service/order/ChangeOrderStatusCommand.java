package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;

public class ChangeOrderStatusCommand {
    private Long orderId;
    private OrderStatus newStatus;
    private String message;

    public ChangeOrderStatusCommand(Long orderId, OrderStatus newStatus, String message) {
        this.orderId = orderId;
        this.newStatus = newStatus;
        this.message = message;
    }

    public Long getOrderId() { return orderId; }
    public OrderStatus getNewStatus() { return newStatus; }
    public String getMessage() { return message; }
}
