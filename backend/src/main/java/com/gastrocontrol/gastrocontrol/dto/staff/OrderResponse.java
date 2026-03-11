// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/OrderResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;

import java.time.Instant;
import java.util.List;

/**
 * Response payload for a staff-facing order.
 *
 * <p>Always includes {@link OrderType}. Delivery / pickup snapshots are included
 * only when relevant to the order type. {@code createdAt} is always present and
 * is used by the Kitchen Display System to compute elapsed time and urgency.</p>
 */
public class OrderResponse {

    private Long id;
    private OrderType type;

    /** Present only for DINE_IN orders. */
    private Long tableId;

    private int totalCents;
    private OrderStatus status;

    /**
     * When the order was first created.
     * Serialised as an ISO-8601 string by Jackson ({@code WRITE_DATES_AS_TIMESTAMPS=false}).
     */
    private Instant createdAt;

    /** Present only for DELIVERY orders. */
    private DeliverySnapshotDto delivery;

    /** Present only for TAKE_AWAY orders. */
    private PickupSnapshotDto pickup;

    private List<OrderItemResponse> items;

    /** Notes left by staff, sorted oldest-first. May be empty but never null. */
    private List<OrderNoteResponse> notes;

    // ── Getters / setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public int getTotalCents() { return totalCents; }
    public void setTotalCents(int totalCents) { this.totalCents = totalCents; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public DeliverySnapshotDto getDelivery() { return delivery; }
    public void setDelivery(DeliverySnapshotDto delivery) { this.delivery = delivery; }

    public PickupSnapshotDto getPickup() { return pickup; }
    public void setPickup(PickupSnapshotDto pickup) { this.pickup = pickup; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public List<OrderNoteResponse> getNotes() { return notes; }
    public void setNotes(List<OrderNoteResponse> notes) { this.notes = notes; }

    // ── Nested DTOs ──────────────────────────────────────────────────────────

    /**
     * Represents a single line item within an order.
     */
    public static class OrderItemResponse {

        /**
         * Primary key of the order line (order_items.id).
         * This is NOT the product id — it uniquely identifies this line within the order.
         */
        private Long id;
        private Long productId;
        private String name;
        private int quantity;
        private int unitPriceCents;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public int getUnitPriceCents() { return unitPriceCents; }
        public void setUnitPriceCents(int unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    }

    /**
     * Represents a single staff note attached to an order.
     *
     * <p>Edit audit fields:</p>
     * <ul>
     *   <li>{@code originalNote} — the text before the very first edit; {@code null} if never edited.</li>
     *   <li>{@code editedAt} — timestamp of the most recent edit; {@code null} if never edited.</li>
     * </ul>
     */
    public static class OrderNoteResponse {

        private Long id;
        private String note;
        private String authorRole;
        private Instant createdAt;

        /**
         * The original text before the first edit. {@code null} if the note has never been edited.
         * Once set on the backend this value is frozen — it always reflects the first original.
         */
        private String originalNote;

        /**
         * When the note was last edited. {@code null} if the note has never been edited.
         * Serialised as ISO-8601 by Jackson.
         */
        private Instant editedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public String getAuthorRole() { return authorRole; }
        public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public String getOriginalNote() { return originalNote; }
        public void setOriginalNote(String originalNote) { this.originalNote = originalNote; }

        public Instant getEditedAt() { return editedAt; }
        public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
    }
}