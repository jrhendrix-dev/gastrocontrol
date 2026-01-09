// src/main/java/com/gastrocontrol/gastrocontrol/mapper/order/OrderMapper.java
package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderItemDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;

import java.util.stream.Collectors;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderDto toDto(OrderJpaEntity order) {
        var items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPriceCents()
                ))
                .collect(Collectors.toList());

        Long tableId = order.getDiningTable() == null ? null : order.getDiningTable().getId();

        return new OrderDto(
                order.getId(),
                tableId,
                order.getTotalCents(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getClosedAt(),
                items
        );
    }
}
