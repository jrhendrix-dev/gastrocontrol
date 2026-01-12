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
 * Staff-facing order response.
 * Includes delivery/pickup snapshots when relevant and (optionally) payment summary.
 */
public class OrderResponse {

    private Long id;
    private OrderType type;

    /** Present only for DINE_IN orders */
    private Long tableId;

    private int totalCents;
    private OrderStatus status;

    /** Present only for DELIVERY orders */
    private DeliverySnapshotDto delivery;

    /** Present only for TAKE_AWAY orders */
    private PickupSnapshotDto pickup;

    private List<OrderItemResponse> items;

    /** Present only when an order has a payment row */
    private PaymentSummary payment;

    // ---------- Getters / setters ----------

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

    public DeliverySnapshotDto getDelivery() { return delivery; }
    public void setDelivery(DeliverySnapshotDto delivery) { this.delivery = delivery; }

    public PickupSnapshotDto getPickup() { return pickup; }
    public void setPickup(PickupSnapshotDto pickup) { this.pickup = pickup; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public PaymentSummary getPayment() { return payment; }
    public void setPayment(PaymentSummary payment) { this.payment = payment; }

    // ---------- Nested DTOs ----------

    public static class OrderItemResponse {
        private Long productId;
        private String name;
        private int quantity;
        private int unitPriceCents;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public int getUnitPriceCents() { return unitPriceCents; }
        public void setUnitPriceCents(int unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    }

    public static class PaymentSummary {
        private PaymentProvider provider;
        private PaymentStatus status;
        private int amountCents;
        private String currency;
        private String checkoutSessionId;
        private String paymentIntentId;
        private Instant updatedAt;

        public PaymentProvider getProvider() { return provider; }
        public void setProvider(PaymentProvider provider) { this.provider = provider; }

        public PaymentStatus getStatus() { return status; }
        public void setStatus(PaymentStatus status) { this.status = status; }

        public int getAmountCents() { return amountCents; }
        public void setAmountCents(int amountCents) { this.amountCents = amountCents; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getCheckoutSessionId() { return checkoutSessionId; }
        public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }

        public String getPaymentIntentId() { return paymentIntentId; }
        public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}
