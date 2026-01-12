// src/main/java/com/gastrocontrol/gastrocontrol/application/service/payment/ReconcilePendingPaymentsService.java
package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReconcilePendingPaymentsService {

    private static final Duration DEFAULT_STALE_AFTER = Duration.ofMinutes(2);

    private final PaymentRepository paymentRepository;
    private final ReconcilePaymentService reconcilePaymentService;

    public ReconcilePendingPaymentsService(
            PaymentRepository paymentRepository,
            ReconcilePaymentService reconcilePaymentService
    ) {
        this.paymentRepository = paymentRepository;
        this.reconcilePaymentService = reconcilePaymentService;
    }

    /**
     * Reconcile up to N "stale" pending payments.
     *
     * "Stale" means updated_at is older than {@code staleAfter}. This avoids hammering Stripe
     * immediately after creating/resuming a checkout session.
     */
    public BatchReconcileResult reconcileStalePending(int limit, Duration staleAfter) {
        int capped = Math.max(1, Math.min(limit, 50));
        Duration effectiveStaleAfter = (staleAfter == null || staleAfter.isNegative() || staleAfter.isZero())
                ? DEFAULT_STALE_AFTER
                : staleAfter;

        Instant cutoff = Instant.now().minus(effectiveStaleAfter);

        var candidates = paymentRepository.findTop50ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PaymentStatus.REQUIRES_PAYMENT,
                cutoff
        );

        if (candidates.size() > capped) {
            candidates = candidates.subList(0, capped);
        }

        int attempted = 0;
        int updated = 0;
        int alreadyOk = 0;
        int failed = 0;

        List<ItemResult> items = new ArrayList<>();

        for (var p : candidates) {
            Long orderId = p.getOrder().getId();
            attempted++;

            try {
                var r = reconcilePaymentService.handle(orderId);

                boolean changed =
                        r.oldPaymentStatus() != r.newPaymentStatus()
                                || r.oldOrderStatus() != r.newOrderStatus();

                if (r.alreadyPaid() || !changed) {
                    alreadyOk++;
                } else {
                    updated++;
                }

                items.add(new ItemResult(orderId, true, r.message()));
            } catch (Exception ex) {
                failed++;
                items.add(new ItemResult(orderId, false, ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        }

        return new BatchReconcileResult(attempted, updated, alreadyOk, failed, cutoff, items);
    }

    /** Backwards-compatible default: 2 minutes stale. */
    public BatchReconcileResult reconcilePending(int limit) {
        return reconcileStalePending(limit, DEFAULT_STALE_AFTER);
    }

    public record BatchReconcileResult(
            int attempted,
            int updated,
            int alreadyOk,
            int failed,
            Instant cutoffUsed,
            List<ItemResult> items
    ) {}

    public record ItemResult(
            Long orderId,
            boolean ok,
            String message
    ) {}
}
