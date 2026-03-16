// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/CategoryResponse.java
package com.gastrocontrol.gastrocontrol.dto.manager;

/**
 * Manager-facing representation of a product category.
 *
 * @param id   the category's primary key
 * @param name the display name (unique across all categories)
 */
public record CategoryResponse(Long id, String name) {}