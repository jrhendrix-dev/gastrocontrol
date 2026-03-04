package com.gastrocontrol.gastrocontrol.application.service.order;

/**
 * Result returned by {@link ProcessOrderAdjustmentService} after the financial adjustment
 * for a reopened order has been processed.
 *
 * @param orderId          the adjusted order id
 * @param paidAmountCents  the amount confirmed as paid before the reopen
 * @param newTotalCents    the new order total after item modifications
 * @param deltaCents       the financial delta: {@code newTotalCents - paidAmountCents};
 *                         positive = extra charge, negative = refund, zero = no action
 * @param adjustmentType   human-readable description of the action taken
 *                         (e.g., "REFUND", "EXTRA_CHARGE", "NO_ADJUSTMENT")
 * @param providerReference the gateway-specific id for the refund or new charge,
 *                          or the manual reference; null if delta was zero
 */
public record ProcessOrderAdjustmentResult(
        Long orderId,
        int paidAmountCents,
        int newTotalCents,
        int deltaCents,
        String adjustmentType,
        String providerReference
) {}