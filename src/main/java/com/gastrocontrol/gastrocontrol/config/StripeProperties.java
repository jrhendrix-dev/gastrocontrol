package com.gastrocontrol.gastrocontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String currency,
        String checkoutSuccessUrl,
        String checkoutCancelUrl
) {}
