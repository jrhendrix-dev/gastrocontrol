package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.OrderDto;
import com.gastrocontrol.gastrocontrol.dto.order.OrderItemDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;

import java.util.stream.Collectors;

/**
 * Maps {@link OrderJpaEntity} to {@link OrderDto}.
 *
 * <p>This mapper is used for manager/staff-facing endpoints that return the richer
 * {@link OrderDto} (e.g., reopen, process-adjustment). Item-level endpoints use
 * {@link StaffOrderMapper} instead.</p>
 */
public final class OrderMapper {

    private OrderMapper() {}

    /**
     * Maps an order entity to its DTO representation.
     *
     * <p>The entity's items collection must be initialised (not a lazy proxy) before
     * calling this method. Use {@code orderRepository.findHydratedById()} to ensure this.</p>
     *
     * @param order the order entity to map; must not be null
     * @return a fully-populated {@link OrderDto}
     */
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
                order.isReopened(),
                order.getCreatedAt(),
                order.getClosedAt(),
                items
        );
    }
}