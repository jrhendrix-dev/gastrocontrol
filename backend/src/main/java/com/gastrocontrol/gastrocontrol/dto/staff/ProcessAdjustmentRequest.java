package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/staff/orders/{orderId}/actions/process-adjustment}.
 *
 * <p>After a manager reopens an order and finishes modifying items, this endpoint
 * is called to resolve the financial delta and close the edit window.</p>
 *
 * <p>For MANUAL provider, {@code manualReference} is required (e.g., a card terminal
 * receipt number, or "cash returned to customer").</p>
 *
 * <p>For STRIPE provider, the gateway call is currently stubbed and will return
 * an error until implemented in {@code StripePaymentGateway}.</p>
 */
public class ProcessAdjustmentRequest {

    /**
     * The payment provider to use for the financial adjustment.
     * Must be {@code MANUAL} or {@code STRIPE}.
     */
    @NotNull(message = "provider is required")
    private PaymentProvider provider;

    /**
     * Required for MANUAL provider. A human-readable reference for audit purposes.
     * Examples: "Cash refund given", "Terminal receipt #A-4421", "Comped by manager"
     */
    private String manualReference;

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }

    public String getManualReference() { return manualReference; }
    public void setManualReference(String manualReference) { this.manualReference = manualReference; }
}