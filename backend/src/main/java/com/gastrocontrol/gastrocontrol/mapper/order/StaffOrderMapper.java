// src/main/java/com/gastrocontrol/gastrocontrol/mapper/order/StaffOrderMapper.java
package com.gastrocontrol.gastrocontrol.mapper.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link OrderJpaEntity} to {@link OrderResponse}.
 *
 * <p>The entity's {@code items} and {@code notes} collections must be
 * initialised (not lazy proxies) before calling this mapper.
 * Use {@code orderRepository.findHydratedById()} to ensure this.</p>
 *
 * <p>An optional {@link PaymentJpaEntity} may be supplied to populate the
 * {@code paymentProvider} and {@code paymentStatus} summary fields.
 * Pass {@code null} when the payment row is not available.</p>
 */
public final class StaffOrderMapper {

    private StaffOrderMapper() {}

    /**
     * Maps an order entity to the staff-facing response DTO without payment info.
     *
     * @param order the fully-hydrated order entity; must not be null
     * @return a populated {@link OrderResponse} with null payment fields
     */
    public static OrderResponse toResponse(OrderJpaEntity order) {
        return toResponse(order, null);
    }

    /**
     * Maps an order entity and its payment to the staff-facing response DTO.
     *
     * @param order   the fully-hydrated order entity; must not be null
     * @param payment the order's payment row, or {@code null} if not available
     * @return a populated {@link OrderResponse}
     */
    public static OrderResponse toResponse(OrderJpaEntity order, PaymentJpaEntity payment) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setType(order.getType());
        r.setTableId(order.getDiningTable() == null ? null : order.getDiningTable().getId());
        r.setTotalCents(order.getTotalCents());
        r.setStatus(order.getStatus());
        r.setReopened(order.isReopened());
        r.setCreatedAt(order.getCreatedAt());

        // Payment summary — null-safe
        if (payment != null) {
            r.setPaymentProvider(payment.getProvider());
            r.setPaymentStatus(payment.getStatus());
        }

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

        // Notes
        List<OrderNoteJpaEntity> rawNotes = order.getNotes();
        if (rawNotes != null) {
            r.setNotes(rawNotes.stream().map(n -> {
                OrderResponse.OrderNoteResponse note = new OrderResponse.OrderNoteResponse();
                note.setId(n.getId());
                note.setNote(n.getNote());
                note.setAuthorRole(n.getAuthorRole());
                note.setCreatedAt(n.getCreatedAt());
                note.setOriginalNote(n.getOriginalNote());
                note.setEditedAt(n.getEditedAt());
                return note;
            }).collect(Collectors.toList()));
        } else {
            r.setNotes(Collections.emptyList());
        }

        return r;
    }
}