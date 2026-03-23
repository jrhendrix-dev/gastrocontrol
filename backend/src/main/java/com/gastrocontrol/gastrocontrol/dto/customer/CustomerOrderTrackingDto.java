// src/main/java/com/gastrocontrol/gastrocontrol/dto/customer/CustomerOrderTrackingDto.java
package com.gastrocontrol.gastrocontrol.dto.customer;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

import java.time.Instant;
import java.util.List;

/**
 * Public-facing order tracking DTO.
 *
 * <p>Intentionally omits staff notes, payment details, and internal audit
 * fields — only the data a customer needs to track their order is exposed.</p>
 */
public record CustomerOrderTrackingDto(

        long id,
        OrderType type,
        OrderStatus status,

        /** ISO-8601 timestamp of when the order was placed. */
        Instant createdAt,

        /** Estimated minutes remaining — null until kitchen starts preparation. */
        Integer estimatedMinutesRemaining,

        /** Pickup name for TAKE_AWAY orders; null otherwise. */
        String pickupName,

        /** Delivery address line for DELIVERY orders; null otherwise. */
        String deliveryAddressLine1,

        /** Delivery city; null for non-DELIVERY orders. */
        String deliveryCity,

        /** The items in this order. */
        List<TrackingItem> items

) {
    /**
     * A single line item shown on the tracking page.
     *
     * @param name     product name
     * @param quantity quantity ordered
     */
    public record TrackingItem(String name, int quantity) {}
}