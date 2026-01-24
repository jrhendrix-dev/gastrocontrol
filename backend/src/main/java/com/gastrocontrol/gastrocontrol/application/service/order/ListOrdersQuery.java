// src/main/java/com/gastrocontrol/gastrocontrol/service/order/ListOrdersQuery.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

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

    /** Present only when type=DINE_IN; filters by diningTable id. */
    private final Long tableId;

    public ListOrdersQuery(List<OrderStatus> statuses, OrderType type, Instant createdFrom, Instant createdTo) {
        this(statuses, type, createdFrom, createdTo, null);
    }

    public ListOrdersQuery(List<OrderStatus> statuses, OrderType type, Instant createdFrom, Instant createdTo, Long tableId) {
        this.statuses = statuses;
        this.type = type;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.tableId = tableId;
    }

    public List<OrderStatus> getStatuses() { return statuses; }
    public OrderType getType() { return type; }
    public Instant getCreatedFrom() { return createdFrom; }
    public Instant getCreatedTo() { return createdTo; }

    public Long getTableId() { return tableId; }
}