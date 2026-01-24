package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

/**
 * Result for draft order creation.
 */
public class CreateDraftOrderResult {

    private final Long orderId;
    private final OrderType type;
    private final Long tableId;
    private final OrderStatus status;

    public CreateDraftOrderResult(Long orderId, OrderType type, Long tableId, OrderStatus status) {
        this.orderId = orderId;
        this.type = type;
        this.tableId = tableId;
        this.status = status;
    }

    public Long getOrderId() { return orderId; }
    public OrderType getType() { return type; }
    public Long getTableId() { return tableId; }
    public OrderStatus getStatus() { return status; }
}
