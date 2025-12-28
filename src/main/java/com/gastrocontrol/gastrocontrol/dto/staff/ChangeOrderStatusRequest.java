// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/ChangeOrderStatusRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class ChangeOrderStatusRequest {

    @NotNull(message = "newStatus is required")
    private OrderStatus newStatus;

    // optional message to be stored in order_events.message
    private String message;

    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
