package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Represents a payment attempt or confirmation for an Order.
 *
 * Invariants:
 * - amountCents must be > 0
 * - currency must not be blank
 * - provider and status must never be null
 * - One payment per order (enforced via unique constraint)
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

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

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

    protected PaymentJpaEntity() {
        // JPA only
    }

    public PaymentJpaEntity(
            OrderJpaEntity order,
            PaymentProvider provider,
            PaymentStatus status,
            int amountCents,
            String currency
    ) {
        if (order == null) throw new IllegalArgumentException("order is required");
        if (provider == null) throw new IllegalArgumentException("provider is required");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be > 0");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");

        this.order = order;
        this.provider = provider;
        this.status = status;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    @PrePersist
    protected void onCreate() {
        validateInvariants();
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
        if (this.version == null) this.version = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        validateInvariants();
        this.updatedAt = Instant.now();
    }

    private void validateInvariants() {
        if (provider == null) throw new IllegalStateException("provider is required");
        if (status == null) throw new IllegalStateException("status is required");
        if (amountCents <= 0) throw new IllegalStateException("amountCents must be > 0");
        if (currency == null || currency.isBlank()) throw new IllegalStateException("currency is required");
    }

    // ----------------- Getters -----------------

    public Long getId() { return id; }
    public OrderJpaEntity getOrder() { return order; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getCheckoutSessionId() { return checkoutSessionId; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public String getManualReference() { return manualReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ----------------- Controlled Mutations -----------------

    public void setStatus(PaymentStatus status) {
        if (status == null) throw new IllegalArgumentException("status is required");
        this.status = status;
    }

    public void setCheckoutSessionId(String checkoutSessionId) {
        this.checkoutSessionId = checkoutSessionId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public void setManualReference(String manualReference) {
        this.manualReference = (manualReference == null || manualReference.isBlank())
                ? null
                : manualReference.trim();
    }
}
