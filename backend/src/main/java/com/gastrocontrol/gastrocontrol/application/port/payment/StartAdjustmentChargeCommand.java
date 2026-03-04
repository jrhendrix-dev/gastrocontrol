package com.gastrocontrol.gastrocontrol.application.port.payment;

import java.util.Map;
import java.util.Objects;

/**
 * Command to start a new payment charge for the positive delta on a reopened order.
 *
 * <p>This is distinct from the original checkout flow. It creates a fresh PaymentIntent
 * (not a Checkout Session) because we already have the customer and do not need to
 * redirect them through a payment page — the staff handles this at the counter.</p>
 *
 * @param orderId        internal order id, used for metadata and idempotency
 * @param amountCents    the additional amount to charge in the smallest currency unit; must be &gt; 0
 * @param currency       ISO 4217 currency code, lower-case (e.g., {@code "eur"})
 * @param description    human-readable description shown on the payment receipt
 * @param idempotencyKey caller-supplied key; recommended: {@code "adj-order-{orderId}-{timestamp}"}
 * @param metadata       optional provider metadata (key/value pairs, max 50 for Stripe)
 */
public record StartAdjustmentChargeCommand(
        Long orderId,
        int amountCents,
        String currency,
        String description,
        String idempotencyKey,
        Map<String, String> metadata
) {
    /**
     * Compact canonical constructor — validates required fields eagerly.
     */
    public StartAdjustmentChargeCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be > 0, got: " + amountCents);
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }
}