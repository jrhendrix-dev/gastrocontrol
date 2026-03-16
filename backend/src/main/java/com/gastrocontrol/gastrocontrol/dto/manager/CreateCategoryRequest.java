// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/CreateCategoryRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new product category.
 */
public class CreateCategoryRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must not exceed 120 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}