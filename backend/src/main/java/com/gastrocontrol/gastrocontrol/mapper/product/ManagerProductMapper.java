// src/main/java/com/gastrocontrol/gastrocontrol/mapper/product/ManagerProductMapper.java
package com.gastrocontrol.gastrocontrol.mapper.product;

import com.gastrocontrol.gastrocontrol.dto.manager.ManagerProductResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;

/**
 * Maps {@link ProductJpaEntity} to the manager-facing {@link ManagerProductResponse}.
 *
 * <p>Includes discontinuation audit fields and {@code imageUrl} that the
 * public catalog mapper omits.</p>
 */
public final class ManagerProductMapper {

    private ManagerProductMapper() {}

    /**
     * Maps a product entity to a manager response DTO.
     *
     * @param p the product entity; must not be {@code null}
     * @return a fully-populated {@link ManagerProductResponse}
     */
    public static ManagerProductResponse toResponse(ProductJpaEntity p) {
        ManagerProductResponse r = new ManagerProductResponse();
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

        r.setDiscontinuedAt(p.getDiscontinuedAt());
        r.setDiscontinuedReason(p.getDiscontinuedReason());

        if (p.getDiscontinuedBy() != null) {
            r.setDiscontinuedByEmail(p.getDiscontinuedBy().getEmail());
        }

        return r;
    }
}