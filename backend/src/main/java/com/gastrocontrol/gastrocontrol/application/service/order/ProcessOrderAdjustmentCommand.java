package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;

/**
 * Command to process the financial adjustment for an order that has been reopened
 * and modified.
 *
 * <p>The service computes the delta between what was paid and the new order total,
 * then takes the appropriate financial action:</p>
 * <ul>
 *   <li>delta &gt; 0 → extra charge (new PaymentIntent or manual reference)</li>
 *   <li>delta &lt; 0 → partial refund</li>
 *   <li>delta == 0 → no financial action, just close the edit window</li>
 * </ul>
 *
 * @param orderId         the order to adjust
 * @param provider        how to handle the financial adjustment (STRIPE or MANUAL)
 * @param manualReference optional reference for MANUAL adjustments (e.g., "Card terminal receipt #123")
 */
public record ProcessOrderAdjustmentCommand(
        Long orderId,
        PaymentProvider provider,
        String manualReference
) {}