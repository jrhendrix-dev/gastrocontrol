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
        if (req.getType() == null) throw new ValidationException(Map.of("type", "Type is required"));

        if (req.getType() == OrderType.DINE_IN) {
            throw new ValidationException(Map.of("type", "Customer checkout does not support DINE_IN"));
        }

        DeliverySnapshotDto delivery = null;
        if (req.getType() == OrderType.DELIVERY) {
            if (req.getDelivery() == null) throw new ValidationException(Map.of("delivery", "Delivery is required"));
            delivery = new DeliverySnapshotDto(
                    req.getDelivery().getName(),
                    req.getDelivery().getPhone(),
                    req.getDelivery().getAddressLine1(),
                    req.getDelivery().getAddressLine2(),
                    req.getDelivery().getCity(),
                    req.getDelivery().getPostalCode(),
                    req.getDelivery().getNotes()
            );
        }

        PickupSnapshotDto pickup = null;
        if (req.getType() == OrderType.TAKE_AWAY) {
            if (req.getPickup() == null) throw new ValidationException(Map.of("pickup", "Pickup is required"));
            pickup = new PickupSnapshotDto(
                    req.getPickup().getName(),
                    req.getPickup().getPhone(),
                    req.getPickup().getNotes()
            );
        }

        CreateOrderCommand cmd = new CreateOrderCommand(
                req.getType(),
                null,
                delivery,
                pickup,
                req.getItems().stream()
                        .map(i -> new CreateOrderCommand.CreateOrderItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList()),
                OrderStatus.PENDING_PAYMENT
        );

        var created = createOrderService.handle(cmd);

        // Create payment row (idempotency anchor is order_id unique)
        var order = orderRepository.findById(created.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order disappeared after create: " + created.getOrderId()));

        PaymentJpaEntity payment = new PaymentJpaEntity(
                order,
                PaymentProvider.STRIPE,
                PaymentStatus.REQUIRES_PAYMENT,
                created.getTotalCents(),
                currency
        );
        paymentRepository.save(payment);

        var checkout = paymentGateway.startCheckout(new CheckoutStartCommand(
                created.getOrderId(),
                created.getTotalCents(),
                currency,
                "GastroControl order #" + created.getOrderId(),
                Map.of("orderId", String.valueOf(created.getOrderId()))
        ));

        payment.setCheckoutSessionId(checkout.checkoutSessionId());
        payment.setPaymentIntentId(checkout.paymentIntentId());

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

        return new CustomerCheckoutResult(created.getOrderId(), checkout.checkoutUrl());
    }

    public record CustomerCheckoutResult(long orderId, String checkoutUrl) {}
}
