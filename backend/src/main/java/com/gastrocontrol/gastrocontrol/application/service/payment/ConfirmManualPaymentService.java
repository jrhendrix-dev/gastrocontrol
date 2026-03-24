// src/main/java/com/gastrocontrol/gastrocontrol/application/service/payment/ConfirmManualPaymentService.java
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

/**
 * Confirms a manual (cash) payment for an order.
 *
 * <p>Supports DINE_IN, TAKE_AWAY, and DELIVERY order types.
 * TAKE_AWAY and DELIVERY cash orders are created by {@link com.gastrocontrol.gastrocontrol.application.service.customer.CustomerCashCheckoutService}
 * and confirmed here when staff physically collects the cash at pickup or delivery.</p>
 */
@Service
public class ConfirmManualPaymentService {

    private final OrderRepository      orderRepository;
    private final PaymentRepository    paymentRepository;
    private final OrderEventRepository orderEventRepository;

    public ConfirmManualPaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.orderRepository      = orderRepository;
        this.paymentRepository    = paymentRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Confirms a manual cash payment for the given order.
     *
     * <p>Supports DINE_IN, TAKE_AWAY, and DELIVERY orders.
     * The payment must already exist with provider MANUAL, or a new MANUAL
     * payment row will be created.</p>
     *
     * @param orderId         the order to confirm payment for
     * @param manualReference optional reference string (e.g. "Efectivo", "Cash on delivery")
     * @return the updated payment details
     * @throws ValidationException if the order has a non-MANUAL payment provider
     * @throws NotFoundException   if the order does not exist
     */
    @Transactional
    public ConfirmManualPaymentResponse handle(Long orderId, String manualReference) {
        if (orderId == null) {
            throw new ValidationException(Map.of("orderId", "Order id is required"));
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // Manual payment is supported for all non-Stripe order types.
        // DINE_IN: staff collects cash at the table.
        // TAKE_AWAY: staff collects cash when customer picks up.
        // DELIVERY: staff/driver collects cash on delivery.
        if (order.getType() == null) {
            throw new ValidationException(Map.of("type", "Order type is missing"));
        }

        int amountCents = order.getTotalCents();
        if (amountCents <= 0) {
            throw new ValidationException(Map.of(
                    "amountCents", "Order total must be > 0 to confirm payment"
            ));
        }

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

        // Enforce provider consistency — cannot confirm STRIPE orders as manual
        if (payment.getProvider() != PaymentProvider.MANUAL) {
            throw new ValidationException(Map.of(
                    "provider",
                    "Order already has a non-manual payment provider: " + payment.getProvider()
            ));
        }

        payment.setManualReference(
                (manualReference == null || manualReference.isBlank()) ? null : manualReference.trim()
        );
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "PAYMENT_MANUAL_CONFIRMED",
                order.getStatus(),
                order.getStatus(),
                "Manual payment confirmed" + (payment.getManualReference() != null
                        ? " (" + payment.getManualReference() + ")" : ""),
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