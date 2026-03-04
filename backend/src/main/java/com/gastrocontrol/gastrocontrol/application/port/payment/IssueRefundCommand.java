package com.gastrocontrol.gastrocontrol.application.port.payment;

import java.util.Objects;

/**
 * Command to issue a (partial or full) refund against a previously confirmed payment.
 *
 * <p>The gateway implementation is responsible for mapping this to the provider-specific
 * refund API (e.g., Stripe {@code Refund.create()}).</p>
 *
 * @param paymentIntentId the Stripe PaymentIntent id (pi_...) that was charged
 * @param amountCents     the amount to refund in the smallest currency unit; must be &gt; 0
 *                        and &lt;= the original charged amount
 * @param reason          optional human-readable reason (for audit / Stripe metadata)
 * @param idempotencyKey  caller-supplied key to make the refund call idempotent;
 *                        recommended format: {@code "refund-order-{orderId}-{timestamp}"}
 */
public record IssueRefundCommand(
        String paymentIntentId,
        int amountCents,
        String reason,
        String idempotencyKey
) {
    /**
     * Compact canonical constructor — validates non-null / positive constraints eagerly.
     */
    public IssueRefundCommand {
        Objects.requireNonNull(paymentIntentId, "paymentIntentId must not be null");
        if (paymentIntentId.isBlank()) throw new IllegalArgumentException("paymentIntentId must not be blank");
        if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be > 0, got: " + amountCents);
    }
}