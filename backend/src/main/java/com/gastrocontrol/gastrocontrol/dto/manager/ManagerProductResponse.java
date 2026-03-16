// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/ManagerProductResponse.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import java.time.Instant;

/**
 * Manager-facing product representation.
 *
 * <p>Extends the staff view with discontinuation audit fields that managers
 * need for product management but staff do not: {@code discontinuedAt},
 * {@code discontinuedReason}, and {@code discontinuedByEmail}.</p>
 */
public class ManagerProductResponse {

    private Long id;
    private String name;
    private String description;
    private int priceCents;
    private boolean active;
    private Long categoryId;
    private String categoryName;

    /** Timestamp when the product was discontinued. {@code null} if currently active. */
    private Instant discontinuedAt;

    /** Optional reason provided when discontinuing. {@code null} if active or no reason given. */
    private String discontinuedReason;

    /** Email of the manager who discontinued the product. {@code null} if active. */
    private String discontinuedByEmail;

    // ── Getters / setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Instant getDiscontinuedAt() { return discontinuedAt; }
    public void setDiscontinuedAt(Instant discontinuedAt) { this.discontinuedAt = discontinuedAt; }

    public String getDiscontinuedReason() { return discontinuedReason; }
    public void setDiscontinuedReason(String discontinuedReason) { this.discontinuedReason = discontinuedReason; }

    public String getDiscontinuedByEmail() { return discontinuedByEmail; }
    public void setDiscontinuedByEmail(String discontinuedByEmail) { this.discontinuedByEmail = discontinuedByEmail; }
}