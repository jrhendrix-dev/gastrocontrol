// src/main/java/com/gastrocontrol/gastrocontrol/application/service/customer/CustomerCashCheckoutService.java
package com.gastrocontrol.gastrocontrol.application.service.customer;

import com.gastrocontrol.gastrocontrol.application.service.order.CreateOrderCommand;
import com.gastrocontrol.gastrocontrol.application.service.order.CreateOrderService;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.*;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerCheckoutRequest;
import com.gastrocontrol.gastrocontrol.dto.order.DeliverySnapshotDto;
import com.gastrocontrol.gastrocontrol.dto.order.PickupSnapshotDto;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles customer cash (pay-on-pickup / pay-on-delivery) orders.
 *
 * <p>Unlike {@link StartCustomerCheckoutService} which creates a Stripe session,
 * this service:</p>
 * <ol>
 *   <li>Creates the order in {@code DRAFT} status.</li>
 *   <li>Immediately submits it to the kitchen ({@code DRAFT → PENDING}).</li>
 *   <li>Records a {@code MANUAL / REQUIRES_PAYMENT} payment row — staff will
 *       confirm receipt of cash at pickup or delivery.</li>
 * </ol>
 *
 * <p>Supports {@code TAKE_AWAY} and {@code DELIVERY} order types only.
 * {@code DINE_IN} cash orders are handled by the POS flow.</p>
 */
@Service
public class CustomerCashCheckoutService {

    private final CreateOrderService   createOrderService;
    private final OrderRepository      orderRepository;
    private final PaymentRepository    paymentRepository;
    private final OrderEventRepository orderEventRepository;

    public CustomerCashCheckoutService(
            CreateOrderService createOrderService,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.createOrderService   = createOrderService;
        this.orderRepository      = orderRepository;
        this.paymentRepository    = paymentRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Result returned after a successful cash checkout.
     *
     * @param orderId the newly created and submitted order id
     */
    public record CashCheckoutResult(long orderId, String trackingToken) {}

    /**
     * Creates, submits, and records a MANUAL payment for a customer cash order.
     *
     * @param req the checkout request (TAKE_AWAY or DELIVERY, with pickup/delivery details)
     * @return the result containing the new order id
     * @throws ValidationException if the request is invalid or type is DINE_IN
     */
    @Transactional
    public CashCheckoutResult handle(CustomerCheckoutRequest req) {
        if (req.getType() == null) {
            throw new ValidationException(Map.of("type", "Type is required"));
        }
        if (req.getType() == OrderType.DINE_IN) {
            throw new ValidationException(Map.of(
                    "type", "Cash checkout does not support DINE_IN — use the POS flow"
            ));
        }

        DeliverySnapshotDto delivery = null;
        PickupSnapshotDto   pickup   = null;

        if (req.getType() == OrderType.DELIVERY) {
            if (req.getDelivery() == null) {
                throw new ValidationException(Map.of("delivery", "Delivery details are required"));
            }
            var d = req.getDelivery();
            delivery = new DeliverySnapshotDto(
                    d.getName(), d.getPhone(), d.getAddressLine1(),
                    d.getAddressLine2(), d.getCity(), d.getPostalCode(), d.getNotes()
            );
        }

        if (req.getType() == OrderType.TAKE_AWAY) {
            if (req.getPickup() == null) {
                throw new ValidationException(Map.of("pickup", "Pickup details are required"));
            }
            var p = req.getPickup();
            pickup = new PickupSnapshotDto(p.getName(), p.getPhone(), p.getNotes());
        }

        // ── Step 1: Create order in PENDING (skip DRAFT for cash) ──────────
        // We use PENDING directly so it appears in the kitchen immediately.
        CreateOrderCommand cmd = new CreateOrderCommand(
                req.getType(),
                null,      // no tableId for customer orders
                delivery,
                pickup,
                req.getItems().stream()
                        .map(i -> new CreateOrderCommand.CreateOrderItem(
                                i.getProductId(), i.getQuantity()
                        ))
                        .collect(Collectors.toList()),
                OrderStatus.PENDING   // goes straight to kitchen
        );

        var created = createOrderService.handle(cmd);

        OrderJpaEntity order = orderRepository.findById(created.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order disappeared after creation"));

        // ── Step 2: Record MANUAL payment (pending cash confirmation by staff) ──
        PaymentJpaEntity payment = new PaymentJpaEntity(
                order,
                PaymentProvider.MANUAL,
                PaymentStatus.REQUIRES_PAYMENT,   // staff confirms at pickup/delivery
                order.getTotalCents(),
                "eur"
        );
        paymentRepository.save(payment);

        // ── Step 3: Audit event ────────────────────────────────────────────────
        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "CUSTOMER_CASH_CHECKOUT",
                null,
                order.getStatus(),
                "Customer placed cash order — payment pending at " +
                        (req.getType() == OrderType.TAKE_AWAY ? "pickup" : "delivery"),
                "CUSTOMER",
                null,
                null
        ));

        return new CashCheckoutResult(order.getId(), order.getTrackingToken());
    }
}