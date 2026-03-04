package com.gastrocontrol.gastrocontrol.dto.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object representing an order, used in manager/staff-facing responses
 * that require richer context than the standard {@code OrderResponse}.
 *
 * <p>Includes the {@code reopened} flag so clients can conditionally show the
 * "process adjustment" UI when an order is in its edit window.</p>
 */
public class OrderDto {
    private final Long id;
    private final Long tableId;
    private final int totalCents;
    private final OrderStatus status;
    private final boolean reopened;
    private final Instant createdAt;
    private final Instant closedAt;
    private final List<OrderItemDto> items;

    /**
     * @param id          the order id
     * @param tableId     the assigned dining table id, or null for TAKE_AWAY / DELIVERY
     * @param totalCents  current order total in the smallest currency unit
     * @param status      current order status
     * @param reopened    true if the order is in the post-reopen edit window
     * @param createdAt   when the order was created
     * @param closedAt    when the order was closed (FINISHED / CANCELLED), or null
     * @param items       the order line items
     */
    public OrderDto(
            Long id,
            Long tableId,
            int totalCents,
            OrderStatus status,
            boolean reopened,
            Instant createdAt,
            Instant closedAt,
            List<OrderItemDto> items
    ) {
        this.id = id;
        this.tableId = tableId;
        this.totalCents = totalCents;
        this.status = status;
        this.reopened = reopened;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public Long getTableId() { return tableId; }
    public int getTotalCents() { return totalCents; }
    public OrderStatus getStatus() { return status; }

    /**
     * Returns {@code true} if the order is currently in the post-reopen edit window.
     * Clients should display the "Process Adjustment" action when this is true.
     *
     * @return whether the edit window is open
     */
    public boolean isReopened() { return reopened; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<OrderItemDto> getItems() { return items; }
}