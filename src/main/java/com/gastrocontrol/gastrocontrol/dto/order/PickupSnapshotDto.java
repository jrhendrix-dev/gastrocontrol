// src/main/java/com/gastrocontrol/gastrocontrol/dto/order/PickupSnapshotDto.java
package com.gastrocontrol.gastrocontrol.dto.order;

/**
 * Snapshot of pickup information captured at order creation time.
 * <p>
 * This is intentionally a snapshot (not a Customer entity) so that the order keeps the
 * exact handoff details even if a customer's data changes later.
 */
public record PickupSnapshotDto(
        String name,
        String phone,
        String notes
) {}
