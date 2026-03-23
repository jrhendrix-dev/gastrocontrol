// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/ListOrdersQuery.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

import java.time.Instant;
import java.util.List;

/**
 * Filtering parameters for listing orders.
 *
 * <p>All fields are optional. Use {@code createdFrom}/{@code createdTo} to filter
 * by when orders were placed. Use {@code closedFrom}/{@code closedTo} to filter
 * by when orders were finalized — this is the correct filter for revenue dashboards,
 * as it captures orders placed days ago but finished today.</p>
 */
public class ListOrdersQuery {

    private final List<OrderStatus> statuses;
    private final OrderType         type;
    private final Instant           createdFrom;
    private final Instant           createdTo;
    private final Long              tableId;

    private Instant closedFrom;
    private Instant closedTo;

    /**
     * Convenience constructor without tableId.
     */
    public ListOrdersQuery(
            List<OrderStatus> statuses,
            OrderType type,
            Instant createdFrom,
            Instant createdTo
    ) {
        this(statuses, type, createdFrom, createdTo, null);
    }

    /**
     * Full constructor.
     */
    public ListOrdersQuery(
            List<OrderStatus> statuses,
            OrderType type,
            Instant createdFrom,
            Instant createdTo,
            Long tableId
    ) {
        this.statuses    = statuses;
        this.type        = type;
        this.createdFrom = createdFrom;
        this.createdTo   = createdTo;
        this.tableId     = tableId;
    }

    public List<OrderStatus> getStatuses()  { return statuses; }
    public OrderType         getType()      { return type; }
    public Instant           getCreatedFrom() { return createdFrom; }
    public Instant           getCreatedTo()   { return createdTo; }
    public Long              getTableId()   { return tableId; }

    /** Lower bound for closedAt — orders finalized on or after this time. */
    public Instant getClosedFrom() { return closedFrom; }
    public void setClosedFrom(Instant closedFrom) { this.closedFrom = closedFrom; }

    /** Upper bound for closedAt — orders finalized before this time. */
    public Instant getClosedTo() { return closedTo; }
    public void setClosedTo(Instant closedTo) { this.closedTo = closedTo; }
}