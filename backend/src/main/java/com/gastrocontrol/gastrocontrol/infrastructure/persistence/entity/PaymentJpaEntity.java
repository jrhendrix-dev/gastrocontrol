package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Represents a payment attempt or confirmation for an Order.
 *
 * <p>Invariants:</p>
 * <ul>
 *   <li>{@code amountCents} must be &gt; 0 (enforced by DB CHECK constraint)</li>
 *   <li>{@code paidAmountCents} is NULL until status transitions to SUCCEEDED</li>
 *   <li>{@code currency} must not be blank</li>
 *   <li>{@code provider} and {@code status} must never be null</li>
 *   <li>One payment per order (enforced via unique constraint)</li>
 * </ul>
 *
 * <p>{@code amountCents} is what was originally requested.
 * {@code paidAmountCents} is what was actually confirmed by the gateway.
 * These diverge only in edge cases (e.g., partial payment, currency rounding).
 * {@code ProcessOrderAdjustmentService} uses {@code paidAmountCents} to compute the
 * financial delta when an order is reopened and modified.</p>
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_order_id", columnNames = {"order_id"}),
                @UniqueConstraint(name = "uk_payments_checkout_session_id", columnNames = {"checkout_session_id"})
        }
)
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_order"))
    private OrderJpaEntity order;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    /** The amount requested at the time the payment record was created, in cents. */
    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    /**
     * The amount actually confirmed by the payment gateway, in cents.
     *
     * <p>NULL until status is SUCCEEDED. Set in:</p>
     * <ul>
     *   <li>{@code HandleStripeCheckoutWebhookService} when Stripe confirms payment</li>
     *   <li>{@code ConfirmManualPaymentService} when staff confirms cash/card</li>
     *   <li>{@code ReconcilePaymentService} when polling Stripe</li>
     * </ul>
     */
    @Column(name = "paid_amount_cents")
    private Integer paidAmountCents;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "checkout_session_id", length = 120)
    private String checkoutSessionId;

    @Column(name = "payment_intent_id", length = 120)
    private String paymentIntentId;

    @Column(name = "manual_reference", length = 120)
    private String manualReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    protected PaymentJpaEntity() {}

    /**
     * Creates a new payment record for the given order.
     *
     * @param order       the order this payment is for
     * @param provider    the payment provider (STRIPE / MANUAL)
     * @param status      the initial status (typically REQUIRES_PAYMENT)
     * @param amountCents the requested amount in the smallest currency unit
     * @param currency    ISO 4217 currency code, lower-case (e.g., "eur")
     */
    public PaymentJpaEntity(
            OrderJpaEntity order,
            PaymentProvider provider,
            PaymentStatus status,
            int amountCents,
            String currency
    ) {
        this.order = order;
        this.provider = provider;
        this.status = status;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
        if (this.version == null) this.version = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------- Getters / setters --------

    public Long getId() { return id; }

    public OrderJpaEntity getOrder() { return order; }

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }

    /**
     * Returns the amount actually paid, or {@code null} if payment has not yet been confirmed.
     *
     * @return confirmed paid amount in cents, or null
     */
    public Integer getPaidAmountCents() { return paidAmountCents; }

    /**
     * Records the amount actually confirmed by the payment provider.
     *
     * <p>Should only be set when transitioning to {@code PaymentStatus.SUCCEEDED}.</p>
     *
     * @param paidAmountCents the confirmed amount in cents; must be &gt; 0
     */
    public void setPaidAmountCents(Integer paidAmountCents) { this.paidAmountCents = paidAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCheckoutSessionId() { return checkoutSessionId; }
    public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getManualReference() { return manualReference; }
    public void setManualReference(String manualReference) { this.manualReference = manualReference; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Integer getVersion() { return version; }
}