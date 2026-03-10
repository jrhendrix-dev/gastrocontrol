// src/main/java/com/gastrocontrol/gastrocontrol/mapper/order/StaffOrderMapper.java
package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link OrderJpaEntity} to {@link OrderResponse}.
 *
 * <p>The entity's {@code items} and {@code notes} collections must be
 * initialised (not lazy proxies) before calling this mapper.
 * Use {@code orderRepository.findHydratedById()} to ensure this.</p>
 */
public final class StaffOrderMapper {

    private StaffOrderMapper() {}

    /**
     * Maps an order entity — including its items and notes — to the staff-facing response DTO.
     *
     * @param order the fully-hydrated order entity; must not be null
     * @return a populated {@link OrderResponse}
     */
    public static OrderResponse toResponse(OrderJpaEntity order) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setType(order.getType());
        r.setTableId(order.getDiningTable() == null ? null : order.getDiningTable().getId());
        r.setTotalCents(order.getTotalCents());
        r.setStatus(order.getStatus());
        r.setCreatedAt(order.getCreatedAt()); // ← fixes "hace NaN h" on the KDS

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
        if (order.getPickupName() != null
                || order.getPickupPhone() != null
                || order.getPickupNotes() != null) {
            r.setPickup(new PickupSnapshotDto(
                    order.getPickupName(),
                    order.getPickupPhone(),
                    order.getPickupNotes()
            ));
        } else {
            r.setPickup(null);
        }

        // Items
        r.setItems(order.getItems().stream().map(i -> {
            OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
            item.setId(i.getId());
            item.setProductId(i.getProduct().getId());
            item.setName(i.getProduct().getName());
            item.setQuantity(i.getQuantity());
            item.setUnitPriceCents(i.getUnitPriceCents());
            return item;
        }).collect(Collectors.toList()));

        // Notes — guard against unloaded lazy collection
        List<OrderNoteJpaEntity> rawNotes = order.getNotes();
        if (rawNotes != null) {
            r.setNotes(rawNotes.stream().map(n -> {
                OrderResponse.OrderNoteResponse note = new OrderResponse.OrderNoteResponse();
                note.setId(n.getId());
                note.setNote(n.getNote());
                note.setAuthorRole(n.getAuthorRole());
                note.setCreatedAt(n.getCreatedAt());
                return note;
            }).collect(Collectors.toList()));
        } else {
            r.setNotes(Collections.emptyList());
        }

        return r;
    }
}