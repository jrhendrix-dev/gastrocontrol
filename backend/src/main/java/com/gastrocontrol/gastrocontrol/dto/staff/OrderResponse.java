// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/OrderResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;

import java.time.Instant;
import java.util.List;

/**
 * Response payload for a staff-facing order.
 *
 * <p>Payment summary fields ({@code paymentProvider}, {@code paymentStatus}) are
 * populated when a payment record exists. Both are {@code null} for orders with
 * no payment row yet. Frontend should treat {@code null} as "payment unknown".</p>
 */
public class OrderResponse {

    private Long id;
    private OrderType type;
    private Long tableId;
    private int totalCents;
    private OrderStatus status;
    private boolean reopened;
    private Instant createdAt;
    private DeliverySnapshotDto delivery;
    private PickupSnapshotDto pickup;
    private List<OrderItemResponse> items;
    private List<OrderNoteResponse> notes;

    /**
     * The payment provider: {@code STRIPE} for online orders, {@code MANUAL} for cash/POS.
     * {@code null} if no payment row exists yet.
     */
    private PaymentProvider paymentProvider;

    /**
     * Current payment status: {@code REQUIRES_PAYMENT} (unpaid) or {@code SUCCEEDED} (paid).
     * {@code null} if no payment row exists yet.
     */
    private PaymentStatus paymentStatus;

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

    public boolean isReopened() { return reopened; }
    public void setReopened(boolean reopened) { this.reopened = reopened; }

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

    public PaymentProvider getPaymentProvider() { return paymentProvider; }
    public void setPaymentProvider(PaymentProvider paymentProvider) { this.paymentProvider = paymentProvider; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public static class OrderItemResponse {
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

    public static class OrderNoteResponse {
        private Long id;
        private String note;
        private String authorRole;
        private Instant createdAt;
        private String originalNote;
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