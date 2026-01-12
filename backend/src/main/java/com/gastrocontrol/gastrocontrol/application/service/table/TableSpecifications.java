// src/main/java/com/gastrocontrol/gastrocontrol/service/table/TableSpecifications.java
package com.gastrocontrol.gastrocontrol.application.service.table;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.DiningTableJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class TableSpecifications {

    private TableSpecifications() {}

    public static Specification<DiningTableJpaEntity> labelContains(String q) {
        if (q == null || q.trim().isEmpty()) return Specification.where(null);
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("label")), like);
    }
}
