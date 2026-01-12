// src/main/java/com/gastrocontrol/gastrocontrol/application/service/payment/ReconcilePaymentService.java
package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.application.port.payment.CheckoutSessionStatusResult;
import com.gastrocontrol.gastrocontrol.application.port.payment.PaymentGateway;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ReconcilePaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentGateway paymentGateway;

    public ReconcilePaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository,
            PaymentGateway paymentGateway
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public ReconcilePaymentResult handle(Long orderId) {
        if (orderId == null) {
            throw new ValidationException(Map.of("orderId", "Order id is required"));
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        var payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        // If already succeeded locally, nothing to do (idempotent)
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return new ReconcilePaymentResult(
                    orderId,
                    order.getStatus(),
                    order.getStatus(),
                    payment.getStatus(),
                    payment.getStatus(),
                    true,
                    "Payment already SUCCEEDED locally"
            );
        }

        String checkoutSessionId = payment.getCheckoutSessionId();
        if (checkoutSessionId == null || checkoutSessionId.isBlank()) {
            throw new ValidationException(Map.of(
                    "checkoutSessionId",
                    "Payment has no checkoutSessionId; cannot reconcile"
            ));
        }

        CheckoutSessionStatusResult session = paymentGateway.getCheckoutSessionStatus(checkoutSessionId);

        boolean paid = "paid".equalsIgnoreCase(session.paymentStatus());
        PaymentStatus oldPaymentStatus = payment.getStatus();
        OrderStatus oldOrderStatus = order.getStatus();

        if (!paid) {
            // Still unpaid/processing: keep order as-is, mark payment as requires payment
            payment.setStatus(PaymentStatus.REQUIRES_PAYMENT);
            if (session.paymentIntentId() != null && !session.paymentIntentId().isBlank()) {
                payment.setPaymentIntentId(session.paymentIntentId());
            }

            orderEventRepository.save(new OrderEventJpaEntity(
                    order,
                    "PAYMENT_RECONCILE_UNPAID",
                    oldOrderStatus,
                    order.getStatus(),
                    "Reconcile: Stripe session not paid yet (sessionStatus=" + session.sessionStatus()
                            + ", paymentStatus=" + session.paymentStatus() + ")",
                    "SYSTEM",
                    null,
                    null
            ));

            return new ReconcilePaymentResult(
                    orderId,
                    oldOrderStatus,
                    order.getStatus(),
                    oldPaymentStatus,
                    payment.getStatus(),
                    false,
                    "Stripe session not paid (sessionStatus=" + session.sessionStatus()
                            + ", paymentStatus=" + session.paymentStatus() + ")"
            );
        }

        // Paid -> flip payment and (if needed) order status
        payment.setStatus(PaymentStatus.SUCCEEDED);
        if (session.paymentIntentId() != null && !session.paymentIntentId().isBlank()) {
            payment.setPaymentIntentId(session.paymentIntentId());
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING);
        }

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "PAYMENT_SUCCEEDED",
                oldOrderStatus,
                order.getStatus(),
                "Reconcile: Stripe payment confirmed",
                "SYSTEM",
                null,
                null
        ));

        return new ReconcilePaymentResult(
                orderId,
                oldOrderStatus,
                order.getStatus(),
                oldPaymentStatus,
                payment.getStatus(),
                false,
                "Stripe payment confirmed"
        );
    }

    public record ReconcilePaymentResult(
            Long orderId,
            OrderStatus oldOrderStatus,
            OrderStatus newOrderStatus,
            PaymentStatus oldPaymentStatus,
            PaymentStatus newPaymentStatus,
            boolean alreadyPaid,
            String message
    ) {}
}
