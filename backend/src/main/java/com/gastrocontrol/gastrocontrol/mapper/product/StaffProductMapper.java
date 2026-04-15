// src/main/java/com/gastrocontrol/gastrocontrol/mapper/product/StaffProductMapper.java
package com.gastrocontrol.gastrocontrol.mapper.product;

import com.gastrocontrol.gastrocontrol.dto.staff.ProductResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;

/**
 * Maps {@link ProductJpaEntity} to the staff-facing {@link ProductResponse}.
 */
public final class StaffProductMapper {

    private StaffProductMapper() {}

    /**
     * Maps a product entity to a staff response DTO.
     *
     * @param p the product entity; must not be null
     * @return a fully-populated {@link ProductResponse}
     */
    public static ProductResponse toResponse(ProductJpaEntity p) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setImageUrl(p.getImageUrl());
        r.setPriceCents(p.getPriceCents());
        r.setActive(p.isActive());

        if (p.getCategory() != null) {
            r.setCategoryId(p.getCategory().getId());
            r.setCategoryName(p.getCategory().getName());
        }

        return r;
    }
}