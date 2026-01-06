package com.gastrocontrol.gastrocontrol.dto.catalog;

import java.util.List;

public record CatalogCategoryDto(
        long id,
        String name,
        List<CatalogProductDto> products
) {}
