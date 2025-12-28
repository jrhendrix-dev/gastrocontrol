package com.gastrocontrol.gastrocontrol.entity;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderEventReasonCode;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "order_events")
public class OrderEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private OrderStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50)
    private OrderEventReasonCode reasonCode;

    protected OrderEventJpaEntity() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    //multiple constructors at the moment to keep retrocompatible

    public OrderEventJpaEntity(
            OrderJpaEntity order,
            String eventType,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String message,
            String actorRole
    ) {
        this(order, eventType, fromStatus, toStatus, message, actorRole, null, null);
    }

    public OrderEventJpaEntity(
            OrderJpaEntity order,
            String eventType,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String message,
            String actorRole,
            OrderEventReasonCode reasonCode
    ) {
        this(order, eventType, fromStatus, toStatus, message, actorRole, null, reasonCode);
    }

    public OrderEventJpaEntity(
            OrderJpaEntity order,
            String eventType,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String message,
            String actorRole,
            Long actorUserId,
            OrderEventReasonCode reasonCode
    ) {
        this.order = order;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.message = message;
        this.actorRole = actorRole;
        this.actorUserId = actorUserId;   // null for now
        this.reasonCode = reasonCode;     // null allowed
    }


    public Long getId() { return id; }
    public OrderJpaEntity getOrder() { return order; }
    public String getEventType() { return eventType; }
    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public String getMessage() { return message; }
    public String getActorRole() { return actorRole; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getActorUserId() { return actorUserId; }
    public OrderEventReasonCode getReasonCode() { return reasonCode; }
}
