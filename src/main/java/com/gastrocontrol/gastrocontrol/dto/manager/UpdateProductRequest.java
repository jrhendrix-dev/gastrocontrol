// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/UpdateProductRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.Min;

public class UpdateProductRequest {

    private String name;
    private String description;

    @Min(0)
    private Integer priceCents;

    private Long categoryId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
