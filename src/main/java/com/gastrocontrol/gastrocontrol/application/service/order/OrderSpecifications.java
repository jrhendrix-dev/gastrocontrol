// src/main/java/com/gastrocontrol/gastrocontrol/service/order/OrderSpecifications.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<OrderJpaEntity> build(ListOrdersQuery q) {
        Specification<OrderJpaEntity> spec = Specification.where(null);

        if (q == null) return spec;

        List<OrderStatus> statuses = q.getStatuses();
        OrderType type = q.getType();
        Instant from = q.getCreatedFrom();
        Instant to = q.getCreatedTo();

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        }

        return spec;
    }
}
