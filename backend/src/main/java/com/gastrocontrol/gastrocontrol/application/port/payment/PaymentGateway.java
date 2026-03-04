package com.gastrocontrol.gastrocontrol.application.port.payment;

/**
 * Port for interacting with a payment gateway provider.
 *
 * <p>All methods are provider-agnostic. The concrete adapter (e.g., {@code StripePaymentGateway})
 * translates these commands into provider-specific API calls.</p>
 *
 * <p>Current implementations:</p>
 * <ul>
 *   <li>{@code StripePaymentGateway} — live Stripe integration</li>
 * </ul>
 */
public interface PaymentGateway {

    /**
     * Creates a Checkout Session for the customer to pay for their order.
     *
     * @param command the checkout parameters
     * @return session id, checkout URL, and optional PaymentIntent id
     */
    CheckoutStartResult startCheckout(CheckoutStartCommand command);

    /**
     * Retrieves the current status of a Checkout Session from the provider.
     *
     * @param checkoutSessionId the provider's session id (e.g., Stripe {@code cs_...})
     * @return current session and payment statuses
     */
    CheckoutSessionStatusResult getCheckoutSessionStatus(String checkoutSessionId);

    /**
     * Issues a (partial or full) refund against a previously confirmed payment.
     *
     * <p>This is used when an order is reopened, items are removed, and the new total
     * is lower than the amount already paid. The difference is returned to the customer.</p>
     *
     * <p><strong>Note:</strong> This method is currently stubbed in the Stripe adapter.
     * It will throw {@link UnsupportedOperationException} until the Stripe Refunds API
     * is wired up in {@code StripePaymentGateway}.</p>
     *
     * @param command refund parameters including PaymentIntent id and amount
     * @return refund id and provider status
     * @throws UnsupportedOperationException if the adapter has not implemented this yet
     */
    IssueRefundResult issueRefund(IssueRefundCommand command);

    /**
     * Starts a new charge for the positive delta when an order is reopened and items
     * are added, increasing the total beyond what was already paid.
     *
     * <p><strong>Note:</strong> This method is currently stubbed in the Stripe adapter.
     * It will throw {@link UnsupportedOperationException} until the Stripe PaymentIntents API
     * is wired up in {@code StripePaymentGateway}.</p>
     *
     * @param command adjustment charge parameters
     * @return new PaymentIntent id and provider status
     * @throws UnsupportedOperationException if the adapter has not implemented this yet
     */
    StartAdjustmentChargeResult startAdjustmentCharge(StartAdjustmentChargeCommand command);
}