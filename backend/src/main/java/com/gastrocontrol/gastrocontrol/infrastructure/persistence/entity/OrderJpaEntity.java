package com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderNoteJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a restaurant order.
 *
 * <p>An order may originate from the POS (DINE_IN draft ticket) or from the
 * customer-facing app (TAKE_AWAY / DELIVERY, starting in DRAFT pending payment).</p>
 *
 * <p>The {@code reopened} flag is a transient "edit window" sentinel. It is set to
 * {@code true} by {@code ReopenOrderService} and cleared by
 * {@code ProcessOrderAdjustmentService} once the financial delta has been resolved.
 * While {@code reopened = true}, item modifications are permitted even for paid orders,
 * but the order cannot be moved to FINISHED.</p>
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dining_table_id")
    private DiningTableJpaEntity diningTable;

    @Column(name = "total_cents", nullable = false)
    private int totalCents = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Indicates the order is in an edit-unlocked state following a reopen.
     *
     * <p>Set to {@code true} by {@code ReopenOrderService}.
     * Cleared to {@code false} by {@code ProcessOrderAdjustmentService}
     * once the financial adjustment (refund or extra charge) has been handled.</p>
     *
     * <p>While {@code true}:
     * <ul>
     *   <li>Item modifications (add/remove/update qty) are permitted despite SUCCEEDED payment.</li>
     *   <li>Transitioning to FINISHED is blocked.</li>
     * </ul>
     * </p>
     */
    @Column(name = "reopened", nullable = false)
    private boolean reopened = false;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

     @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
     @OrderBy("createdAt ASC")
     private List<OrderNoteJpaEntity> notes = new ArrayList<>();

    // Delivery snapshot fields (nullable; required only for DELIVERY orders)
    @Column(name = "delivery_name", length = 120)
    private String deliveryName;

    @Column(name = "delivery_phone", length = 30)
    private String deliveryPhone;

    @Column(name = "delivery_address_line1", length = 190)
    private String deliveryAddressLine1;

    @Column(name = "delivery_address_line2", length = 190)
    private String deliveryAddressLine2;

    @Column(name = "delivery_city", length = 120)
    private String deliveryCity;

    @Column(name = "delivery_postal_code", length = 20)
    private String deliveryPostalCode;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    // Pickup snapshot fields (nullable; required only for TAKE_AWAY orders)
    @Column(name = "pickup_name", length = 120)
    private String pickupName;

    @Column(name = "pickup_phone", length = 30)
    private String pickupPhone;

    @Column(name = "pickup_notes", length = 500)
    private String pickupNotes;

    public OrderJpaEntity() {}

    /**
     * Use this in services; keeps invariants together.
     *
     * @param type         the order type (DINE_IN / TAKE_AWAY / DELIVERY)
     * @param status       the initial status
     * @param diningTable  the assigned table (required for DINE_IN, null otherwise)
     */
    public OrderJpaEntity(OrderType type, OrderStatus status, DiningTableJpaEntity diningTable) {
        this.type = type;
        this.status = status;
        this.diningTable = diningTable;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) this.status = OrderStatus.PENDING;
        if (this.type == null) this.type = OrderType.DINE_IN;
        if (this.version == null) this.version = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------- Domain helpers --------

    /**
     * Adds an item to this order and sets the bidirectional reference.
     *
     * @param item the item to add; ignored if null
     */
    public void addItem(OrderItemJpaEntity item) {
        if (item == null) return;
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Removes an item from this order.
     *
     * <p>Because the owning side of the relationship is {@code OrderItemJpaEntity.order},
     * we must null it out so JPA can correctly apply orphan removal.</p>
     *
     * @param item the item to remove; ignored if null
     */
    public void removeItem(OrderItemJpaEntity item) {
        if (item == null) return;
        items.remove(item);
        item.setOrder(null);
    }

    // -------- Getters / setters --------

    public Long getId() { return id; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public DiningTableJpaEntity getDiningTable() { return diningTable; }
    public void setDiningTable(DiningTableJpaEntity diningTable) { this.diningTable = diningTable; }

    public int getTotalCents() { return totalCents; }
    public void setTotalCents(int totalCents) { this.totalCents = totalCents; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    /**
     * Returns {@code true} if this order is in the post-reopen edit window.
     *
     * @return whether the order has been reopened and is pending financial adjustment
     */
    public boolean isReopened() { return reopened; }

    /**
     * Sets the reopened flag.
     *
     * <p>Should only be called by {@code ReopenOrderService} (set to {@code true}) and
     * {@code ProcessOrderAdjustmentService} (set to {@code false}).</p>
     *
     * @param reopened the new flag value
     */
    public void setReopened(boolean reopened) { this.reopened = reopened; }

    public Integer getVersion() { return version; }

    public List<OrderItemJpaEntity> getItems() { return items; }

    public String getDeliveryName() { return deliveryName; }
    public void setDeliveryName(String deliveryName) { this.deliveryName = deliveryName; }

    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }

    public String getDeliveryAddressLine1() { return deliveryAddressLine1; }
    public void setDeliveryAddressLine1(String deliveryAddressLine1) { this.deliveryAddressLine1 = deliveryAddressLine1; }

    public String getDeliveryAddressLine2() { return deliveryAddressLine2; }
    public void setDeliveryAddressLine2(String deliveryAddressLine2) { this.deliveryAddressLine2 = deliveryAddressLine2; }

    public String getDeliveryCity() { return deliveryCity; }
    public void setDeliveryCity(String deliveryCity) { this.deliveryCity = deliveryCity; }

    public String getDeliveryPostalCode() { return deliveryPostalCode; }
    public void setDeliveryPostalCode(String deliveryPostalCode) { this.deliveryPostalCode = deliveryPostalCode; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getPickupPhone() { return pickupPhone; }
    public void setPickupPhone(String pickupPhone) { this.pickupPhone = pickupPhone; }

    public String getPickupNotes() { return pickupNotes; }
    public void setPickupNotes(String pickupNotes) { this.pickupNotes = pickupNotes; }

     public List<OrderNoteJpaEntity> getNotes() { return notes; }
}