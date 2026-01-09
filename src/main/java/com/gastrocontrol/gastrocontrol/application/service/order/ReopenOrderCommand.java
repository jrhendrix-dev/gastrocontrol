package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderEventReasonCode;

public class ReopenOrderCommand {
    private final Long orderId;
    private final OrderEventReasonCode reasonCode;
    private final String message;

    public ReopenOrderCommand(Long orderId, OrderEventReasonCode reasonCode, String message) {
        this.orderId = orderId;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public Long getOrderId() { return orderId; }
    public OrderEventReasonCode getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
}
