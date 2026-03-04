package com.gastrocontrol.gastrocontrol.application.port.payment;

/**
 * Result returned by {@link PaymentGateway#issueRefund(IssueRefundCommand)} after a
 * refund has been successfully initiated with the payment provider.
 *
 * <p>Note: "initiated" does not mean the money has landed — refunds can be asynchronous.
 * The {@code refundId} can be used to poll or reconcile the refund status later.</p>
 *
 * @param refundId    provider-specific refund identifier (e.g., Stripe {@code re_...})
 * @param amountCents the refunded amount in the smallest currency unit
 * @param status      provider-reported status at initiation time (e.g., {@code "succeeded"}, {@code "pending"})
 */
public record IssueRefundResult(
        String refundId,
        int amountCents,
        String status
) {}