package com.gastrocontrol.gastrocontrol.dto.customer;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CustomerCheckoutRequest {

    @NotNull
    private OrderType type; // TAKE_AWAY or DELIVERY

    @Valid
    private CustomerDeliveryRequest delivery;

    @Valid
    private CustomerPickupRequest pickup;

    @NotEmpty
    @Valid
    private List<CustomerOrderItemRequest> items;

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public CustomerDeliveryRequest getDelivery() { return delivery; }
    public void setDelivery(CustomerDeliveryRequest delivery) { this.delivery = delivery; }

    public CustomerPickupRequest getPickup() { return pickup; }
    public void setPickup(CustomerPickupRequest pickup) { this.pickup = pickup; }

    public List<CustomerOrderItemRequest> getItems() { return items; }
    public void setItems(List<CustomerOrderItemRequest> items) { this.items = items; }

    public static class CustomerOrderItemRequest {
        @NotNull
        private Long productId;

        @NotNull
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
