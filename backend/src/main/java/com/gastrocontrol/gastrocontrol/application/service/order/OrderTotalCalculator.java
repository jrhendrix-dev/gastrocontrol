package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderItemJpaEntity;

import java.util.List;

/**
 * Utility for computing order totals from items.
 */
public final class OrderTotalCalculator {

    private OrderTotalCalculator() {
    }

    /**
     * Computes the order total (cents) from the given items.
     */
    public static int computeTotalCents(List<OrderItemJpaEntity> items) {
        if (items == null || items.isEmpty()) return 0;
        int total = 0;
        for (OrderItemJpaEntity i : items) {
            total += i.getUnitPriceCents() * i.getQuantity();
        }
        return total;
    }
}