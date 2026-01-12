package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;

public record OrderPaymentResponse(
        Long orderId,
        PaymentProvider provider,
        PaymentStatus status,
        int amountCents,
        String currency,
        String checkoutSessionId,
        String paymentIntentId
) {}
