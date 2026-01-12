// src/main/java/com/gastrocontrol/gastrocontrol/service/product/ProductSpecifications.java
package com.gastrocontrol.gastrocontrol.application.service.product;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<ProductJpaEntity> build(ListProductsQuery q) {
        Specification<ProductJpaEntity> spec = Specification.where(null);
        if (q == null) return spec;

        if (q.getActive() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), q.getActive()));
        }

        if (q.getCategoryId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), q.getCategoryId()));
        }

        if (q.getQ() != null && !q.getQ().trim().isEmpty()) {
            String like = "%" + q.getQ().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }

        return spec;
    }
}
