package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.Instant;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    protected PaymentJpaEntity() {}

    public PaymentJpaEntity(OrderJpaEntity order, PaymentProvider provider, PaymentStatus status, int amountCents, String currency) {
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

    public Long getId() { return id; }
    public OrderJpaEntity getOrder() { return order; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getCheckoutSessionId() { return checkoutSessionId; }
    public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    public Instant getUpdatedAt() { return updatedAt; }

}
