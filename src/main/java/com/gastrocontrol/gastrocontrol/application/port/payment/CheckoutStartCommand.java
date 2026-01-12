package com.gastrocontrol.gastrocontrol.application.port.payment;

import java.util.Map;

public record CheckoutStartCommand(
        long orderId,
        int amountCents,
        String currency,
        String description,
        Map<String, String> metadata
) {}
