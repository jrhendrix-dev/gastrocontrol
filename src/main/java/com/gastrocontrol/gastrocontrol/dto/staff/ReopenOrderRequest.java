package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderEventReasonCode;
import jakarta.validation.constraints.NotNull;

public class ReopenOrderRequest {

    @NotNull(message = "reasonCode is required")
    private OrderEventReasonCode reasonCode;

    // optional, but recommended
    private String message;

    public OrderEventReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(OrderEventReasonCode reasonCode) { this.reasonCode = reasonCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
