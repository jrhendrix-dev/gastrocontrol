package com.gastrocontrol.gastrocontrol.dto.customer;

public record CustomerCheckoutResponse(
        long orderId,
        String checkoutUrl
) {}
