// src/main/java/com/gastrocontrol/gastrocontrol/dto/catalog/CatalogProductDto.java
package com.gastrocontrol.gastrocontrol.dto.catalog;

/**
 * Public-facing product DTO returned by the catalog endpoints.
 *
 * <p>Safe for unauthenticated use — never exposes cost, admin, or audit fields.</p>
 *
 * @param id          the product's primary key
 * @param name        display name
 * @param description optional marketing description
 * @param imageUrl    server-relative URL of the product image, or {@code null}
 * @param priceCents  price in euro cents
 * @param categoryId  the owning category ID, or {@code null} if uncategorised
 * @param categoryName the owning category name, or {@code null} if uncategorised
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