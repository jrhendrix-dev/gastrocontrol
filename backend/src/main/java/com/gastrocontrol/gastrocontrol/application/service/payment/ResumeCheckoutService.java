package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.application.port.payment.CheckoutStartCommand;
import com.gastrocontrol.gastrocontrol.application.port.payment.PaymentGateway;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.dto.staff.ResumeCheckoutResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResumeCheckoutService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentGateway paymentGateway;

    public ResumeCheckoutService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository,
            PaymentGateway paymentGateway
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public ResumeCheckoutResponse handle(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        var payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        // If already paid, nothing to resume
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return ResumeCheckoutResponse.paid(
                    orderId,
                    payment.getProvider(),
                    payment.getStatus(),
                    payment.getCheckoutSessionId(),
                    payment.getPaymentIntentId()
            );
        }

        // Only Stripe for now (guardrail for future providers)
        if (payment.getProvider() != PaymentProvider.STRIPE) {
            throw new IllegalStateException("Resume checkout not supported for provider: " + payment.getProvider());
        }

        // Ensure order is in pending payment state (or keep as-is if you prefer)
        if (order.getStatus() != OrderStatus.DRAFT) {
            order.setStatus(OrderStatus.DRAFT);
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("orderId", String.valueOf(orderId));
        metadata.put("source", "staff_resume_checkout");

        var result = paymentGateway.startCheckout(new CheckoutStartCommand(
                orderId,
                order.getTotalCents(),
                payment.getCurrency(),
                "GastroControl order #" + orderId,
                metadata
        ));

        // Persist the new session as the active one
        payment.setCheckoutSessionId(result.checkoutSessionId());
        payment.setPaymentIntentId(result.paymentIntentId());
        payment.setStatus(PaymentStatus.REQUIRES_PAYMENT);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "CHECKOUT_RESUMED",
                order.getStatus(),
                order.getStatus(),
                "Checkout resumed (new Stripe session generated)",
                "STAFF",
                null,
                null
        ));

        return ResumeCheckoutResponse.started(
                orderId,
                payment.getProvider(),
                payment.getStatus(),
                result.checkoutSessionId(),
                result.checkoutUrl(),
                result.paymentIntentId()
        );
    }
}
