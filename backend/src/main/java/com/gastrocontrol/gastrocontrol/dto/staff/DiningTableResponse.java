// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/DiningTableResponse.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;

public class DiningTableResponse {

    private Long id;
    private String label;

    /**
     * Optional summary of the currently active order for this table.
     * Present only when the caller requests include=activeOrder.
     */
    private ActiveOrderSummary activeOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public ActiveOrderSummary getActiveOrder() { return activeOrder; }
    public void setActiveOrder(ActiveOrderSummary activeOrder) { this.activeOrder = activeOrder; }

    /** Lightweight active order payload for table screens. */
    public static class ActiveOrderSummary {
        private Long orderId;
        private OrderStatus status;
        private int totalCents;

        private PaymentStatus paymentStatus;
        private boolean paid;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }

        public int getTotalCents() { return totalCents; }
        public void setTotalCents(int totalCents) { this.totalCents = totalCents; }

        public PaymentStatus getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }
}