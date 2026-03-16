// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/UpdateCategoryRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for renaming an existing product category.
 */
public class UpdateCategoryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}