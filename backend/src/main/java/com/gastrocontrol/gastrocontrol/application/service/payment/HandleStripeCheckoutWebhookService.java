package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.domain.enums.OrderStatus;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderEventJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.OrderJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.entity.PaymentJpaEntity;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderEventRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.OrderRepository;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Handles Stripe Checkout webhooks (checkout.session.completed).
 *
 * <p>Goals:</p>
 * <ul>
 *   <li><b>Idempotent</b>: Stripe may resend events; we must not double-apply.</li>
 *   <li><b>Resilient</b>: If the Payment row wasn't created earlier, upsert it.</li>
 *   <li><b>Consistent</b>: Transition order from PENDING_PAYMENT -> PENDING only when paid.</li>
 * </ul>
 */
@Service
public class HandleStripeCheckoutWebhookService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public HandleStripeCheckoutWebhookService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * Applies the result of a Stripe Checkout Session completion.
     *
     * @param sessionId Stripe Checkout Session id (cs_...)
     * @param paymentStatus Stripe "payment_status" from session ("paid", "unpaid", "no_payment_required", ...)
     * @param orderId internal order id (from client_reference_id or metadata)
     * @param paymentIntentId Stripe PaymentIntent id (pi_...), may be null
     */
    @Transactional
    public void handleCheckoutSessionCompleted(
            String sessionId,
            String paymentStatus,
            Long orderId,
            String paymentIntentId
    ) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        // Order is your internal source of truth
        OrderJpaEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // Upsert Payment:
        // 1) by checkout_session_id (ideal idempotency anchor)
        // 2) else by order_id (because order_id is also unique in your schema)
        PaymentJpaEntity payment = paymentRepository.findByCheckoutSessionId(sessionId)
                .orElseGet(() -> paymentRepository.findByOrder_Id(orderId).orElse(null));

        if (payment == null) {
            // Create minimal payment record (keeps webhook resilient during dev)
            payment = new PaymentJpaEntity(
                    order,
                    PaymentProvider.STRIPE,
                    PaymentStatus.REQUIRES_PAYMENT,
                    order.getTotalCents(),
                    "eur"
            );
        }

        // Ensure checkout_session_id is stored for future idempotency and debugging
        if (payment.getCheckoutSessionId() == null || payment.getCheckoutSessionId().isBlank()) {
            payment.setCheckoutSessionId(sessionId);
        }

        // Attach PI id if present (useful for reconciliation)
        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            payment.setPaymentIntentId(paymentIntentId);
        }

        // Hard idempotency: if already succeeded, do nothing (but still ensure sessionId/PI were stored above)
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            paymentRepository.save(payment);
            return;
        }

        boolean paid = "paid".equalsIgnoreCase(paymentStatus);

        if (!paid) {
            // Completed session but not paid (delayed methods can do this)
            payment.setStatus(PaymentStatus.REQUIRES_PAYMENT);
            paymentRepository.save(payment);

            // Optional event: useful for tracking weird flows
            orderEventRepository.save(new OrderEventJpaEntity(
                    order,
                    "PAYMENT_NOT_CONFIRMED",
                    order.getStatus(),
                    order.getStatus(),
                    "Stripe checkout completed but payment_status=" + paymentStatus + " (sessionId=" + sessionId + ")",
                    "SYSTEM",
                    null,
                    null
            ));
            return;
        }

        // Payment confirmed
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        // Flip order status only if it is still waiting for payment
        OrderStatus before = order.getStatus();
        if (before == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING);
        }
        orderRepository.save(order);

        orderEventRepository.save(new OrderEventJpaEntity(
                order,
                "PAYMENT_SUCCEEDED",
                before,
                order.getStatus(),
                "Stripe payment confirmed (sessionId=" + sessionId + ")",
                "SYSTEM",
                null,
                null
        ));
    }
}
