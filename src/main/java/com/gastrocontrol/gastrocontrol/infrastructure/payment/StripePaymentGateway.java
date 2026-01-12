package com.gastrocontrol.gastrocontrol.infrastructure.payment;

import com.gastrocontrol.gastrocontrol.application.port.payment.*;
import com.gastrocontrol.gastrocontrol.config.StripeProperties;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties props;

    public StripePaymentGateway(StripeProperties props) {
        this.props = props;
    }

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

        // metadata is supported and extremely useful for debugging/reconciliation
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
}
