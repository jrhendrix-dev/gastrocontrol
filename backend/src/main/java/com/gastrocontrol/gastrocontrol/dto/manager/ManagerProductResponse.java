// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/ManagerProductResponse.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import java.time.Instant;

/**
 * Manager-facing product representation.
 *
 * <p>Extends the public catalog view with discontinuation audit fields and the
 * {@code imageUrl} field so the admin UI can display and manage product images.</p>
 */
public class ManagerProductResponse {

    private Long id;
    private String name;
    private String description;

    /** Server-relative URL of the product image, or {@code null} if none uploaded. */
    private String imageUrl;

    private int priceCents;
    private boolean active;
    private Long categoryId;
    private String categoryName;
    private Instant discontinuedAt;
    private String discontinuedReason;
    private String discontinuedByEmail;

    // ── Getters / setters ────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public String getImageUrl()                { return imageUrl; }
    public void setImageUrl(String imageUrl)   { this.imageUrl = imageUrl; }

    public int getPriceCents()                 { return priceCents; }
    public void setPriceCents(int p)           { this.priceCents = p; }

    public boolean isActive()                  { return active; }
    public void setActive(boolean active)      { this.active = active; }

    public Long getCategoryId()                { return categoryId; }
    public void setCategoryId(Long id)         { this.categoryId = id; }

    public String getCategoryName()            { return categoryName; }
    public void setCategoryName(String n)      { this.categoryName = n; }

    public Instant getDiscontinuedAt()         { return discontinuedAt; }
    public void setDiscontinuedAt(Instant t)   { this.discontinuedAt = t; }

    public String getDiscontinuedReason()      { return discontinuedReason; }
    public void setDiscontinuedReason(String r){ this.discontinuedReason = r; }

    public String getDiscontinuedByEmail()     { return discontinuedByEmail; }
    public void setDiscontinuedByEmail(String e){ this.discontinuedByEmail = e; }
}