// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/CreateProductRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProductRequest {

    @NotBlank
    @Size(max = 60, message = "name must not exceed 60 characters")
    private String name;

    @Size(max = 300, message = "description must not exceed 300 characters")
    private String description;

    @Min(0)
    private int priceCents;

    private boolean active = true;

    private Long categoryId;

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
}
