// src/main/java/com/gastrocontrol/gastrocontrol/service/order/ListOrdersQuery.java
package com.gastrocontrol.gastrocontrol.service.order;

import com.gastrocontrol.gastrocontrol.entity.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.entity.enums.OrderType;

import java.time.Instant;
import java.util.List;

/**
 * Filtering parameters for listing orders.
 */
public class ListOrdersQuery {

    private final List<OrderStatus> statuses; // optional
    private final OrderType type;             // optional
    private final Instant createdFrom;        // optional (inclusive)
    private final Instant createdTo;          // optional (exclusive)

    public ListOrdersQuery(List<OrderStatus> statuses, OrderType type, Instant createdFrom, Instant createdTo) {
        this.statuses = statuses;
        this.type = type;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
    }

    public List<OrderStatus> getStatuses() { return statuses; }
    public OrderType getType() { return type; }
    public Instant getCreatedFrom() { return createdFrom; }
    public Instant getCreatedTo() { return createdTo; }
}
