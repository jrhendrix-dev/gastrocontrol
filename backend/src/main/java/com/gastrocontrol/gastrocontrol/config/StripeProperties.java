package com.gastrocontrol.gastrocontrol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe configuration bound from application.yaml (prefix "stripe").
 *
 * Expected env vars (via docker compose):
 * - STRIPE_API_KEY
 * - STRIPE_WEBHOOK_SECRET
 * - STRIPE_CURRENCY
 * - STRIPE_CHECKOUT_SUCCESS_URL
 * - STRIPE_CHECKOUT_CANCEL_URL
 */
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String currency,
        String checkoutSuccessUrl,
        String checkoutCancelUrl,
        boolean verifyWebhookSignature
) {}
