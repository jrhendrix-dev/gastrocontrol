package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;

public class ChangeOrderStatusResult {
    private final Long orderId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public ChangeOrderStatusResult(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getOrderId() { return orderId; }
    public OrderStatus getOldStatus() { return oldStatus; }
    public OrderStatus getNewStatus() { return newStatus; }
}
