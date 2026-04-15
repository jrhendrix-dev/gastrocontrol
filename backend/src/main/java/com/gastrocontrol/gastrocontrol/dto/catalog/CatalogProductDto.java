// src/main/java/com/gastrocontrol/gastrocontrol/dto/catalog/CatalogProductDto.java
package com.gastrocontrol.gastrocontrol.dto.catalog;

/**
 * Public-facing product DTO returned by the catalog endpoints.
 *
 * <p>Safe for unauthenticated use — never includes cost, admin, or discontinuation fields.</p>
 *
 * @param imageUrl server-relative path to the product's hero image
 *                 (e.g. {@code /gastrocontrol/uploads/products/42.webp}),
 *                 or {@code null} when no image has been uploaded
 */
public record CatalogProductDto(
        long id,
        String name,
        String description,
        String imageUrl,
        int priceCents,
        Long categoryId,
        String categoryName
) {}