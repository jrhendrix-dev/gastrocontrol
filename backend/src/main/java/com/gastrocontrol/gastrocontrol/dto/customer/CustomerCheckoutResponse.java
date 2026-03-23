// src/main/java/com/gastrocontrol/gastrocontrol/dto/customer/CustomerCheckoutResponse.java
package com.gastrocontrol.gastrocontrol.dto.customer;

/**
 * Response body for the Stripe checkout endpoint.
 *
 * @param orderId       the newly created order id
 * @param checkoutUrl   the Stripe Checkout redirect URL
 * @param trackingToken opaque UUID for the public tracking URL (/track/{token})
 */
public record CustomerCheckoutResponse(
        long orderId,
        String checkoutUrl,
        String trackingToken
) {}