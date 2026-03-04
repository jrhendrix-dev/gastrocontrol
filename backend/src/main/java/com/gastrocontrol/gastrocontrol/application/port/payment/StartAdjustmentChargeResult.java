package com.gastrocontrol.gastrocontrol.application.port.payment;

/**
 * Result returned by {@link PaymentGateway#startAdjustmentCharge(StartAdjustmentChargeCommand)}
 * after a new charge for the order delta has been initiated with the payment provider.
 *
 * @param paymentIntentId provider-specific PaymentIntent id (e.g., Stripe {@code pi_...})
 * @param amountCents     the charged amount in the smallest currency unit
 * @param status          provider-reported status at creation time
 *                        (e.g., {@code "requires_payment_method"}, {@code "succeeded"})
 * @param clientSecret    optional client secret needed if the UI must confirm the payment
 *                        (present for Stripe PaymentIntents in manual confirmation mode)
 */
public record StartAdjustmentChargeResult(
        String paymentIntentId,
        int amountCents,
        String status,
        String clientSecret
) {}