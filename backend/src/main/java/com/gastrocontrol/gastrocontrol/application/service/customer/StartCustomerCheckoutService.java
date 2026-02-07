package com.gastrocontrol.gastrocontrol.application.service.customer;

import com.gastrocontrol.gastrocontrol.application.port.payment.CheckoutStartCommand;
import com.gastrocontrol.gastrocontrol.application.port.payment.PaymentGateway;
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

@Service
public class StartCustomerCheckoutService {

    private final CreateOrderService createOrderService;
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public StartCustomerCheckoutService(
            CreateOrderService createOrderService,
            PaymentGateway paymentGateway,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.createOrderService = createOrderService;
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public CustomerCheckoutResult start(CustomerCheckoutRequest req, String currency) {

        if (req.getType() == null) {
            throw new ValidationException(Map.of("type", "Type is required"));
        }

        if (req.getType() == OrderType.DINE_IN) {
            throw new ValidationException(Map.of(
                    "type",
                    "Customer checkout does not support DINE_IN"
            ));
        }

        DeliverySnapshotDto delivery = null;
        PickupSnapshotDto pickup = null;

        if (req.getType() == OrderType.DELIVERY) {
            if (req.getDelivery() == null) {
                throw new ValidationException(Map.of("delivery", "Delivery is required"));
            }
            var d = req.getDelivery();
            delivery = new DeliverySnapshotDto(
                    d.getName(),
                    d.getPhone(),
                    d.getAddressLine1(),
                    d.getAddressLine2(),
                    d.getCity(),
                    d.getPostalCode(),
                    d.getNotes()
            );
        }

        if (req.getType() == OrderType.TAKE_AWAY) {
            if (req.getPickup() == null) {
                throw new ValidationException(Map.of("pickup", "Pickup is required"));
            }
            var p = req.getPickup();
            pickup = new PickupSnapshotDto(
                    p.getName(),
                    p.getPhone(),
                    p.getNotes()
            );
        }

        CreateOrderCommand cmd = new CreateOrderCommand(
                req.getType(),
                null,
                delivery,
                pickup,
                req.getItems().stream()
                        .map(i -> new CreateOrderCommand.CreateOrderItem(
                                i.getProductId(),
                                i.getQuantity()
                        ))
                        .collect(Collectors.toList()),
                OrderStatus.DRAFT
        );

        var created = createOrderService.handle(cmd);

        OrderJpaEntity order = orderRepository.findById(created.getOrderId())
                .orElseThrow(() ->
                        new IllegalStateException("Order disappeared after creation")
                );

        // 🔐 Idempotent payment creation (ONE per order)
        PaymentJpaEntity payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseGet(() -> {
                    PaymentJpaEntity p = new PaymentJpaEntity(
                            order,
                            PaymentProvider.STRIPE,
                            PaymentStatus.REQUIRES_PAYMENT,
                            created.getTotalCents(),
                            currency
                    );
                    return paymentRepository.save(p);
                });

        var checkout = paymentGateway.startCheckout(new CheckoutStartCommand(
                order.getId(),
                created.getTotalCents(),
                currency,
                "GastroControl order #" + order.getId(),
                Map.of("orderId", String.valueOf(order.getId()))
        ));

        payment.setCheckoutSessionId(checkout.checkoutSessionId());
        payment.setPaymentIntentId(checkout.paymentIntentId());
        paymentRepository.save(payment);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "CUSTOMER_CHECKOUT_STARTED",
                null,
                order.getStatus(),
                "Customer started checkout",
                "CUSTOMER",
                null,
                null
        ));

        return new CustomerCheckoutResult(order.getId(), checkout.checkoutUrl());
    }

    public record CustomerCheckoutResult(long orderId, String checkoutUrl) {}
}
