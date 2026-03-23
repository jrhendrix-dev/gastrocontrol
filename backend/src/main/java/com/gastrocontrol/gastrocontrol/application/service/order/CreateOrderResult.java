// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/CreateOrderResult.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

import java.util.List;

/**
 * Result returned by {@link CreateOrderService} after an order is successfully created.
 *
 * <p>{@code trackingToken} is an opaque UUID populated for TAKE_AWAY and DELIVERY orders.
 * It is used in the public customer tracking URL ({@code /track/{token}}) so that
 * sequential order ids are never exposed to the public.</p>
 */
public class CreateOrderResult {

    private final Long orderId;
    private final OrderType type;
    private final Long tableId;
    private final int totalCents;
    private final OrderStatus status;
    private final DeliverySnapshotDto delivery;
    private final PickupSnapshotDto pickup;
    private String trackingToken;
    private final List<CreateOrderItemResult> items;

    public CreateOrderResult(
            Long orderId,
            OrderType type,
            Long tableId,
            int totalCents,
            OrderStatus status,
            DeliverySnapshotDto delivery,
            PickupSnapshotDto pickup,
            String trackingToken,
            List<CreateOrderItemResult> items
    ) {
        this.orderId = orderId;
        this.type = type;
        this.tableId = tableId;
        this.totalCents = totalCents;
        this.status = status;
        this.delivery = delivery;
        this.pickup = pickup;
        this.trackingToken = trackingToken;
        this.items = items;
    }

    public Long getOrderId()                    { return orderId; }
    public OrderType getType()                  { return type; }
    public Long getTableId()                    { return tableId; }
    public int getTotalCents()                  { return totalCents; }
    public OrderStatus getStatus()              { return status; }
    public DeliverySnapshotDto getDelivery()    { return delivery; }
    public PickupSnapshotDto getPickup()        { return pickup; }
    public List<CreateOrderItemResult> getItems() { return items; }

    /**
     * Opaque UUID for the public tracking URL.
     * Non-null for TAKE_AWAY and DELIVERY orders; null for DINE_IN.
     */
    public String getTrackingToken()                      { return trackingToken; }
    public void setTrackingToken(String trackingToken)    { this.trackingToken = trackingToken; }

    public static class CreateOrderItemResult {
        private final Long productId;
        private final String name;
        private final int quantity;
        private final int unitPriceCents;

        public CreateOrderItemResult(Long productId, String name, int quantity, int unitPriceCents) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.unitPriceCents = unitPriceCents;
        }

        public Long getProductId()      { return productId; }
        public String getName()         { return name; }
        public int getQuantity()        { return quantity; }
        public int getUnitPriceCents()  { return unitPriceCents; }
    }
}