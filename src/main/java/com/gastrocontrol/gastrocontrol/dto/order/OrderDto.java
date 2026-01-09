package com.gastrocontrol.gastrocontrol.dto.order;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.List;

public class OrderDto {
    private final Long id;
    private final Long tableId;
    private final int totalCents;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant closedAt;
    private final List<OrderItemDto> items;

    public OrderDto(Long id, Long tableId, int totalCents, OrderStatus status,
                    Instant createdAt, Instant closedAt, List<OrderItemDto> items) {
        this.id = id;
        this.tableId = tableId;
        this.totalCents = totalCents;
        this.status = status;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public Long getTableId() { return tableId; }
    public int getTotalCents() { return totalCents; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<OrderItemDto> getItems() { return items; }
}
