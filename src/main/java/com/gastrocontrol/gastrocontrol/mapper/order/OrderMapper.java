package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderItemDto;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;

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

        return new OrderDto(
                order.getId(),
                order.getDiningTable().getId(),
                order.getTotalCents(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getClosedAt(),
                items
        );
    }
}
