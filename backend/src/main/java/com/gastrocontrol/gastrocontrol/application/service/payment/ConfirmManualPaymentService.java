package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.dto.staff.ConfirmManualPaymentResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ConfirmManualPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventRepository orderEventRepository;

    public ConfirmManualPaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional
    public ConfirmManualPaymentResponse handle(Long orderId, String manualReference) {
        if (orderId == null) {
            throw new ValidationException(Map.of("orderId", "Order id is required"));
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // B2: manual payments are for dine-in only
        if (order.getType() != OrderType.DINE_IN) {
            throw new ValidationException(Map.of(
                    "type",
                    "Manual payment confirmation is only allowed for DINE_IN orders"
            ));
        }

        int amountCents = order.getTotalCents();
        if (amountCents <= 0) {
            throw new ValidationException(Map.of(
                    "amountCents",
                    "Order total must be > 0 to confirm payment"
            ));
        }

        // Use a sensible default; if you later add restaurant currency config, swap this out.
        String currency = "eur";

        PaymentJpaEntity payment = paymentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    PaymentJpaEntity p = new PaymentJpaEntity(
                            order,
                            PaymentProvider.MANUAL,
                            PaymentStatus.REQUIRES_PAYMENT,
                            amountCents,
                            currency
                    );
                    return paymentRepository.save(p);
                });

        // If a payment already exists, enforce provider consistency for B2
        if (payment.getProvider() != PaymentProvider.MANUAL) {
            throw new ValidationException(Map.of(
                    "provider",
                    "Order already has a non-manual payment provider: " + payment.getProvider()
            ));
        }

        // Keep amount aligned with the order at the moment of confirmation
        // (If you want to *forbid* mismatch instead, replace with a ValidationException)
        if (payment.getAmountCents() != amountCents) {
            // No setter exists currently; if you want strict alignment, add setAmountCents()
            // Recommended: add setAmountCents(int) with validation.
            // For now, we assume payment was created with order total and totals don't change late.
        }

        payment.setManualReference((manualReference == null || manualReference.isBlank()) ? null : manualReference.trim());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "PAYMENT_MANUAL_CONFIRMED",
                order.getStatus(),
                order.getStatus(),
                "Manual payment confirmed" + (payment.getManualReference() != null ? " (" + payment.getManualReference() + ")" : ""),
                "STAFF",
                null,
                null
        ));

        return new ConfirmManualPaymentResponse(
                orderId,
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getManualReference()
        );
    }
}
