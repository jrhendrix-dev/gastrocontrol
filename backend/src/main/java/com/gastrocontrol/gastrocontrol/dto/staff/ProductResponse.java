// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/ProductResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

/**
 * Staff-facing product representation returned by {@code /api/staff/products}.
 *
 * <p>{@code imageUrl} is a server-relative path to the product's hero image
 * (e.g. {@code /gastrocontrol/uploads/products/42.webp}), or {@code null}
 * when no image has been uploaded. The POS and other staff UIs use this field
 * to render product thumbnails.</p>
 */
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private int priceCents;
    private boolean active;
    private Long categoryId;
    private String categoryName;

    public Long getId()                { return id; }
    public void setId(Long id)         { this.id = id; }

    public String getName()            { return name; }
    public void setName(String name)   { this.name = name; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    /** Server-relative URL of the product image, or {@code null} if none uploaded. */
    public String getImageUrl()                { return imageUrl; }
    public void setImageUrl(String imageUrl)   { this.imageUrl = imageUrl; }

    public int getPriceCents()                 { return priceCents; }
    public void setPriceCents(int priceCents)  { this.priceCents = priceCents; }

    public boolean isActive()                  { return active; }
    public void setActive(boolean active)      { this.active = active; }

    public Long getCategoryId()                    { return categoryId; }
    public void setCategoryId(Long categoryId)     { this.categoryId = categoryId; }

    public String getCategoryName()                      { return categoryName; }
    public void setCategoryName(String categoryName)     { this.categoryName = categoryName; }
}