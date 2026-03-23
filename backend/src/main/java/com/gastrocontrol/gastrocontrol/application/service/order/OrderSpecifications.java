// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/OrderSpecifications.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

/**
 * JPA Specifications for filtering orders.
 *
 * <p>Supports filtering by status, type, table, createdAt range, and closedAt range.
 * The closedAt filter is used by the dashboard to query orders <em>finalized</em>
 * today — regardless of when they were originally created.</p>
 */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    /**
     * Builds a composite specification from the given query parameters.
     * All filters are optional — null values are ignored.
     *
     * @param q the query containing filter parameters
     * @return a combined specification, never null
     */
    public static Specification<OrderJpaEntity> build(ListOrdersQuery q) {
        Specification<OrderJpaEntity> spec = Specification.where(null);

        if (q == null) return spec;

        List<OrderStatus> statuses = q.getStatuses();
        OrderType type             = q.getType();
        Instant createdFrom        = q.getCreatedFrom();
        Instant createdTo          = q.getCreatedTo();
        Instant closedFrom         = q.getClosedFrom();
        Instant closedTo           = q.getClosedTo();
        Long tableId               = q.getTableId();

        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        // createdAt range — used for "orders placed today"
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("createdAt"), createdTo));
        }

        // closedAt range — used for "orders FINISHED today" regardless of creation date
        // This is the correct filter for revenue dashboards.
        if (closedFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("closedAt"), closedFrom));
        }
        if (closedTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("closedAt"), closedTo));
        }

        if (tableId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("diningTable").get("id"), tableId));
        }

        return spec;
    }
}