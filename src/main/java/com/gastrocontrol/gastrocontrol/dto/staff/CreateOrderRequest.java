// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/CreateOrderRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {

    @NotNull
    private OrderType type; // DINE_IN / TAKE_AWAY / DELIVERY

    private Long tableId; // required if DINE_IN

    @Valid
    private DeliveryDetailsRequest delivery; // required if DELIVERY

    @Valid
    private PickupDetailsRequest pickup; // required if TAKE_AWAY

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public DeliveryDetailsRequest getDelivery() { return delivery; }
    public void setDelivery(DeliveryDetailsRequest delivery) { this.delivery = delivery; }

    public PickupDetailsRequest getPickup() { return pickup; }
    public void setPickup(PickupDetailsRequest pickup) { this.pickup = pickup; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        @NotNull
        private Long productId;

        private int quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class DeliveryDetailsRequest {
        private String name;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String postalCode;
        private String notes;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class PickupDetailsRequest {
        private String name;
        private String phone;
        private String notes;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
