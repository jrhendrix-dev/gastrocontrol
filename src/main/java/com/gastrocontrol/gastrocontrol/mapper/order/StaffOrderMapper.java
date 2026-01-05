// src/main/java/com/gastrocontrol/gastrocontrol/mapper/order/StaffOrderMapper.java
package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.entity.OrderJpaEntity;

import java.util.stream.Collectors;

public final class StaffOrderMapper {

    private StaffOrderMapper() {}

    public static OrderResponse toResponse(OrderJpaEntity order) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setType(order.getType());
        r.setTableId(order.getDiningTable() == null ? null : order.getDiningTable().getId());
        r.setTotalCents(order.getTotalCents());
        r.setStatus(order.getStatus());

        // Delivery snapshot
        if (order.getDeliveryName() != null
                || order.getDeliveryPhone() != null
                || order.getDeliveryAddressLine1() != null
                || order.getDeliveryCity() != null
                || order.getDeliveryPostalCode() != null
                || order.getDeliveryNotes() != null) {
            r.setDelivery(new DeliverySnapshotDto(
                    order.getDeliveryName(),
                    order.getDeliveryPhone(),
                    order.getDeliveryAddressLine1(),
                    order.getDeliveryAddressLine2(),
                    order.getDeliveryCity(),
                    order.getDeliveryPostalCode(),
                    order.getDeliveryNotes()
            ));
        } else {
            r.setDelivery(null);
        }

        // Pickup snapshot
        if (order.getPickupName() != null || order.getPickupPhone() != null || order.getPickupNotes() != null) {
            r.setPickup(new PickupSnapshotDto(
                    order.getPickupName(),
                    order.getPickupPhone(),
                    order.getPickupNotes()
            ));
        } else {
            r.setPickup(null);
        }

        r.setItems(order.getItems().stream().map(i -> {
            OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
            item.setProductId(i.getProduct().getId());
            item.setName(i.getProduct().getName());
            item.setQuantity(i.getQuantity());
            item.setUnitPriceCents(i.getUnitPriceCents());
            return item;
        }).collect(Collectors.toList()));

        return r;
    }
}
