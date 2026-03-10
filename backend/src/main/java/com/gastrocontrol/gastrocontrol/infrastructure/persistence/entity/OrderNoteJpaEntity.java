// src/main/java/com/gastrocontrol/gastrocontrol/infrastructure/persistence/entity/OrderNoteJpaEntity.java
package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for the {@code order_notes} table (created in V4 migration).
 *
 * <p>Notes are free-text annotations attached to an order by staff.
 * They appear on the Kitchen Display and on the POS order view so the
 * whole team stays informed of special requests, allergies, or other
 * service instructions.</p>
 *
 * <p>Notes are append-only — there is intentionally no update or delete
 * endpoint, preserving the audit trail of what was communicated.</p>
 */
@Entity
@Table(name = "order_notes")
public class OrderNoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    /**
     * Role of the person who wrote the note (e.g. "STAFF", "MANAGER").
     * Stored as plain text so it remains readable even if roles change.
     */
    @Column(name = "author_role", length = 30)
    private String authorRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderNoteJpaEntity() {}

    /**
     * @param order      the order this note belongs to
     * @param note       the note text; must not be blank
     * @param authorRole the role of the author (e.g. "STAFF")
     */
    public OrderNoteJpaEntity(OrderJpaEntity order, String note, String authorRole) {
        this.order = order;
        this.note = note;
        this.authorRole = authorRole;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public OrderJpaEntity getOrder() { return order; }
    public String getNote() { return note; }
    public String getAuthorRole() { return authorRole; }
    public Instant getCreatedAt() { return createdAt; }
}