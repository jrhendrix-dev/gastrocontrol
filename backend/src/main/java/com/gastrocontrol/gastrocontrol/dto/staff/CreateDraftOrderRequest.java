package com.gastrocontrol.gastrocontrol.dto.staff;


import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import jakarta.validation.constraints.NotNull;

/**
 * Request to open a POS ticket (draft order).
 *
 * The goal is to support "open table" behavior: create an order first,
 * then add items over time, then submit to the kitchen.
 */
public class CreateDraftOrderRequest {

    @NotNull(message = "type is required")
    private OrderType type;

    /**
     * Required when type = DINE_IN.
     */
    private Long tableId;

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }
}