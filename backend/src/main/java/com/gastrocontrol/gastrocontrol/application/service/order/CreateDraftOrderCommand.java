package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

/**
 * Command for creating a draft order (open ticket).
 */
public class CreateDraftOrderCommand {

    private final OrderType type;
    private final Long tableId;

    public CreateDraftOrderCommand(OrderType type, Long tableId) {
        this.type = type;
        this.tableId = tableId;
    }

    public OrderType getType() {
        return type;
    }

    public Long getTableId() {
        return tableId;
    }
}