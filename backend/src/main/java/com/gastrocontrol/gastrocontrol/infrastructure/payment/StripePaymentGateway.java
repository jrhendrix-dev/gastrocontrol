package com.gastrocontrol.gastrocontrol.infrastructure.payment;

import com.gastrocontrol.gastrocontrol.application.port.payment.*;
import com.gastrocontrol.gastrocontrol.config.StripeProperties;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stripe implementation of the {@link PaymentGateway} port.
 *
 * <p>Methods marked "STUB" are not yet wired to the Stripe API.
 * They will throw {@link UnsupportedOperationException} until implemented.
 * The service layer ({@code ProcessOrderAdjustmentService}) uses the manual payment
 * path as its fallback, so stubs do not block operations.</p>
 */
@Component
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties props;

    public StripePaymentGateway(StripeProperties props) {
        this.props = props;
    }

    /** {@inheritDoc} */
    @Override
    public CheckoutStartResult startCheckout(CheckoutStartCommand command) {
        // Stripe expects amounts in the smallest currency unit.
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(command.description())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(command.currency())
                        .setUnitAmount((long) command.amountCents())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(props.checkoutSuccessUrl())
                .setCancelUrl(props.checkoutCancelUrl())
                .setClientReferenceId(String.valueOf(command.orderId()))
                .addLineItem(lineItem);

        if (command.metadata() != null && !command.metadata().isEmpty()) {
            builder.putAllMetadata(command.metadata());
        }

        try {
            Session session = Session.create(builder.build());
            return new CheckoutStartResult(session.getId(), session.getUrl(), session.getPaymentIntent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Stripe Checkout Session", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CheckoutSessionStatusResult getCheckoutSessionStatus(String checkoutSessionId) {
        try {
            Session session = Session.retrieve(checkoutSessionId);
            return new CheckoutSessionStatusResult(
                    session.getId(),
                    session.getStatus(),
                    session.getPaymentStatus(),
                    session.getPaymentIntent()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to retrieve Stripe Checkout Session " + checkoutSessionId, e
            );
        }
    }

    /**
     * STUB — Stripe Refunds API not yet wired.
     *
     * <p>To implement: use {@code com.stripe.model.Refund} with an idempotency key.
     * Example:
     * <pre>{@code
     * RefundCreateParams params = RefundCreateParams.builder()
     *     .setPaymentIntent(command.paymentIntentId())
     *     .setAmount((long) command.amountCents())
     *     .build();
     * RequestOptions options = RequestOptions.builder()
     *     .setIdempotencyKey(command.idempotencyKey())
     *     .build();
     * Refund refund = Refund.create(params, options);
     * return new IssueRefundResult(refund.getId(), refund.getAmount().intValue(), refund.getStatus());
     * }</pre>
     * </p>
     *
     * @throws UnsupportedOperationException always, until implemented
     */
    @Override
    public IssueRefundResult issueRefund(IssueRefundCommand command) {
        throw new UnsupportedOperationException(
                "Stripe refund not yet implemented. " +
                        "Use manual payment adjustment or implement StripePaymentGateway#issueRefund."
        );
    }

    /**
     * STUB — Stripe PaymentIntents adjustment charge not yet wired.
     *
     * <p>To implement: create a new PaymentIntent for the delta amount.
     * Example:
     * <pre>{@code
     * PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
     *     .setAmount((long) command.amountCents())
     *     .setCurrency(command.currency())
     *     .setDescription(command.description())
     *     .putAllMetadata(command.metadata())
     *     .build();
     * RequestOptions options = RequestOptions.builder()
     *     .setIdempotencyKey(command.idempotencyKey())
     *     .build();
     * PaymentIntent pi = PaymentIntent.create(params, options);
     * return new StartAdjustmentChargeResult(pi.getId(), pi.getAmount().intValue(), pi.getStatus(), pi.getClientSecret());
     * }</pre>
     * </p>
     *
     * @throws UnsupportedOperationException always, until implemented
     */
    @Override
    public StartAdjustmentChargeResult startAdjustmentCharge(StartAdjustmentChargeCommand command) {
        throw new UnsupportedOperationException(
                "Stripe adjustment charge not yet implemented. " +
                        "Use manual payment adjustment or implement StripePaymentGateway#startAdjustmentCharge."
        );
    }
}