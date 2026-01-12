package com.gastrocontrol.gastrocontrol.application.port.payment;

public record CheckoutStartResult(
        String checkoutSessionId,
        String checkoutUrl,
        String paymentIntentId
) {}
