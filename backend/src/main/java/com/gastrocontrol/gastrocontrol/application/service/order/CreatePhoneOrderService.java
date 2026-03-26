// src/main/java/com/gastrocontrol/gastrocontrol/application/service/order/CreatePhoneOrderService.java
package com.gastrocontrol.gastrocontrol.application.service.order;

import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Creates a TAKE_AWAY or DELIVERY order from a customer phone call.
 *
 * <p>Phone orders differ from online checkout orders:</p>
 * <ul>
 *   <li>They skip Stripe — payment is collected at pickup or delivery.</li>
 *   <li>They start as {@code DRAFT} so staff can add items via the POS product
 *       panel before sending to kitchen.</li>
 *   <li>No payment row is created at order time (amount is 0 until items are added).
 *       {@link com.gastrocontrol.gastrocontrol.application.service.payment.ConfirmManualPaymentService}
 *       creates the MANUAL payment row when staff confirms cash collection.</li>
 *   <li>Kitchen notes from the call are saved as a proper order note (visible in
 *       the kitchen display) rather than only in the pickup/delivery snapshot.</li>
 * </ul>
 *
 * <p>The entity is built directly rather than delegating to {@link CreateOrderService}
 * because phone orders start with zero items — the items-required validation in
 * {@code CreateOrderService} would reject an empty order.</p>
 */
@Service
public class CreatePhoneOrderService {

    private final OrderRepository      orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final AddOrderNoteService  addOrderNoteService;

    public CreatePhoneOrderService(
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            AddOrderNoteService addOrderNoteService
    ) {
        this.orderRepository      = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.addOrderNoteService  = addOrderNoteService;
    }

    /**
     * Command for creating a phone order.
     *
     * @param type     TAKE_AWAY or DELIVERY (required)
     * @param name     customer name (required)
     * @param phone    customer phone (optional but recommended)
     * @param notes    kitchen notes from the call — saved as an order note (optional)
     * @param address  delivery address — required for DELIVERY
     * @param city     delivery city — required for DELIVERY
     */
    public record PhoneOrderCommand(
            OrderType type,
            String name,
            String phone,
            String notes,
            String address,
            String city
    ) {}

    /**
     * Creates a phone order as DRAFT with customer details.
     * Kitchen notes are saved as a proper order note so they appear in the kitchen display.
     *
     * @param cmd the phone order details
     * @return the id of the newly created order
     * @throws ValidationException if type is DINE_IN or required fields are missing
     */
    @Transactional
    public long handle(PhoneOrderCommand cmd) {
        if (cmd.type() == null) {
            throw new ValidationException(Map.of("type", "Type is required"));
        }
        if (cmd.type() == OrderType.DINE_IN) {
            throw new ValidationException(Map.of("type", "Phone orders cannot be DINE_IN"));
        }
        if (cmd.name() == null || cmd.name().isBlank()) {
            throw new ValidationException(Map.of("name", "Customer name is required"));
        }
        if (cmd.type() == OrderType.DELIVERY) {
            if (cmd.address() == null || cmd.address().isBlank()) {
                throw new ValidationException(Map.of("address", "Delivery address is required"));
            }
            if (cmd.city() == null || cmd.city().isBlank()) {
                throw new ValidationException(Map.of("city", "City is required for delivery"));
            }
        }

        // ── Build entity directly — bypasses the items-required guard ──────────
        OrderJpaEntity order = new OrderJpaEntity(cmd.type(), OrderStatus.DRAFT, null);
        order.setTotalCents(0);
        order.setTrackingToken(UUID.randomUUID().toString());

        if (cmd.type() == OrderType.TAKE_AWAY) {
            order.setPickupName(cmd.name().trim());
            order.setPickupPhone(cmd.phone() != null ? cmd.phone().trim() : null);
        } else {
            order.setDeliveryName(cmd.name().trim());
            order.setDeliveryPhone(cmd.phone() != null ? cmd.phone().trim() : null);
            order.setDeliveryAddressLine1(cmd.address().trim());
            order.setDeliveryCity(cmd.city().trim());
        }

        order = orderRepository.save(order);
        final long orderId = order.getId();

        // ── Kitchen notes saved as order note — visible in kitchen display ──────
        // pickupNotes / deliveryNotes are snapshot fields used for delivery info,
        // not for kitchen communication. Order notes appear in the kitchen KDS.
        if (cmd.notes() != null && !cmd.notes().isBlank()) {
            addOrderNoteService.handle(orderId, cmd.notes().trim(), "STAFF");
        }

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "PHONE_ORDER_CREATED",
                null,
                OrderStatus.DRAFT,
                "Phone order created by staff — cash payment pending at " +
                        (cmd.type() == OrderType.TAKE_AWAY ? "pickup" : "delivery"),
                "STAFF",
                null,
                null
        ));

        return orderId;
    }
}