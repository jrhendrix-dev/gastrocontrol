package com.gastrocontrol.gastrocontrol.dto.staff;

import jakarta.validation.constraints.Min;

/**
 * Request to update an existing order item's quantity.
 */
public class UpdateOrderItemRequest {

    @Min(value = 1, message = "quantity must be >= 1")
    private int quantity;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}