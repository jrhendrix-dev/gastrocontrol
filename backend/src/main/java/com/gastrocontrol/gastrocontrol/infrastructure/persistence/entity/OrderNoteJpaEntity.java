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
 * <h3>Edit audit trail</h3>
 * <p>When a note is edited for the first time, {@code originalNote} is frozen
 * with the text that existed before the edit. Subsequent edits update only
 * {@code note} and {@code editedAt}; {@code originalNote} is never overwritten
 * again. This means the first original is always recoverable.</p>
 *
 * <h3>Delete rules</h3>
 * <p>Delete is only permitted while the parent order is in a pre-kitchen status
 * (DRAFT or PENDING). Once the kitchen has the order ({@code IN_PREPARATION}
 * or beyond) the note is part of the live communication record and must not
 * disappear. This is enforced in {@code DeleteOrderNoteService}.</p>
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
     * The very first version of {@code note}, captured on the first edit.
     * {@code NULL} means the note has never been edited.
     * Immutable after it is set — subsequent edits must not overwrite this field.
     */
    @Column(name = "original_note", columnDefinition = "TEXT")
    private String originalNote;

    /**
     * Timestamp of the most recent edit.
     * {@code NULL} means the note has never been edited.
     */
    @Column(name = "edited_at")
    private Instant editedAt;

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
     * Creates a new note. Call {@link #applyEdit(String)} to update the text later.
     *
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

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId()             { return id; }
    public OrderJpaEntity getOrder(){ return order; }
    public String getNote()         { return note; }
    public String getOriginalNote() { return originalNote; }
    public Instant getEditedAt()    { return editedAt; }
    public String getAuthorRole()   { return authorRole; }
    public Instant getCreatedAt()   { return createdAt; }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    /**
     * Applies an edit to this note's text.
     *
     * <p>On the first call, the current {@code note} text is preserved in
     * {@code originalNote} (frozen for the audit trail) before being replaced.
     * On subsequent calls, only {@code note} and {@code editedAt} are updated;
     * {@code originalNote} is never overwritten again.</p>
     *
     * @param newText the replacement text; must not be blank or exceed 500 chars
     *                (validation is the responsibility of the calling service)
     */
    public void applyEdit(String newText) {
        if (this.originalNote == null) {
            // First edit: freeze the original text as the immutable audit anchor
            this.originalNote = this.note;
        }
        this.note = newText;
        this.editedAt = Instant.now();
    }
}