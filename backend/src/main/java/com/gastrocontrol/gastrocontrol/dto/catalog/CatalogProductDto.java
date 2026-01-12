package com.gastrocontrol.gastrocontrol.dto.catalog;

public record CatalogProductDto(
        long id,
        String name,
        String description,
        int priceCents,
        Long categoryId,
        String categoryName
) {}
