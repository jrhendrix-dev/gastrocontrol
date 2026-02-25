package com.gastrocontrol.gastrocontrol.dto.staff;

public class ConfirmManualPaymentRequest {

    /**
     * Optional reference such as "Cash", "Card terminal", "Ticket #123", etc.
     */
    private String manualReference;

    public String getManualReference() {
        return manualReference;
    }

    public void setManualReference(String manualReference) {
        this.manualReference = manualReference;
    }
}
