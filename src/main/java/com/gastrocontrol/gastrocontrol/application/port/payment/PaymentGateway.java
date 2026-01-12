package com.gastrocontrol.gastrocontrol.application.port.payment;

public interface PaymentGateway {
    CheckoutStartResult startCheckout(CheckoutStartCommand command);

    CheckoutSessionStatusResult getCheckoutSessionStatus(String checkoutSessionId);
}
