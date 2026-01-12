package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;

import java.util.List;
import java.util.Objects;

/**
 * Command object used to request creation of a new order.
 */
public class CreateOrderCommand {

    private final OrderType type;                 // DINE_IN / TAKE_AWAY / DELIVERY
    private final Long tableId;                   // required if DINE_IN
    private final DeliverySnapshotDto delivery;   // required if DELIVERY
    private final PickupSnapshotDto pickup;       // required if TAKE_AWAY
    private final List<CreateOrderItem> items;

    /**
     * Optional initial status override (defaults to PENDING).
     * Use for flows like customer checkout: PENDING_PAYMENT.
     */
    private final OrderStatus initialStatus;

    public CreateOrderCommand(
            OrderType type,
            Long tableId,
            DeliverySnapshotDto delivery,
            PickupSnapshotDto pickup,
            List<CreateOrderItem> items
    ) {
        this(type, tableId, delivery, pickup, items, null);
    }

    public CreateOrderCommand(
            OrderType type,
            Long tableId,
            DeliverySnapshotDto delivery,
            PickupSnapshotDto pickup,
            List<CreateOrderItem> items,
            OrderStatus initialStatus
    ) {
        this.type = type == null ? OrderType.DINE_IN : type;
        this.tableId = tableId;
        this.delivery = delivery;
        this.pickup = pickup;
        this.items = items;
        this.initialStatus = initialStatus;
    }

    public OrderType getType() { return type; }
    public Long getTableId() { return tableId; }
    public DeliverySnapshotDto getDelivery() { return delivery; }
    public PickupSnapshotDto getPickup() { return pickup; }
    public List<CreateOrderItem> getItems() { return items; }
    public OrderStatus getInitialStatus() { return initialStatus; }

    public static class CreateOrderItem {
        private final Long productId;
        private final int quantity;

        public CreateOrderItem(Long productId, int quantity) {
            this.productId = Objects.requireNonNull(productId, "productId must not be null");
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
    }
}
