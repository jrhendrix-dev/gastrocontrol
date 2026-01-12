package com.gastrocontrol.gastrocontrol.application.port.payment;

public record CheckoutSessionStatusResult(
        String checkoutSessionId,
        String sessionStatus,      // open | complete | expired
        String paymentStatus,      // paid | unpaid | no_payment_required
        String paymentIntentId
) {}
