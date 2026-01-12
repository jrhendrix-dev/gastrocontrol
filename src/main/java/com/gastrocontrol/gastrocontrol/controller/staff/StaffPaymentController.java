// src/main/java/com/gastrocontrol/gastrocontrol/controller/staff/StaffPaymentController.java
package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.application.service.payment.ReconcilePaymentService;
import com.gastrocontrol.gastrocontrol.application.service.payment.ReconcilePendingPaymentsService;
import com.gastrocontrol.gastrocontrol.application.service.payment.ResumeCheckoutService;
import com.gastrocontrol.gastrocontrol.dto.staff.ResumeCheckoutResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/payments")
public class StaffPaymentController {

    private final ResumeCheckoutService resumeCheckoutService;
    private final ReconcilePaymentService reconcilePaymentService;
    private final ReconcilePendingPaymentsService reconcilePendingPaymentsService;

    public StaffPaymentController(
            ResumeCheckoutService resumeCheckoutService,
            ReconcilePaymentService reconcilePaymentService,
            ReconcilePendingPaymentsService reconcilePendingPaymentsService
    ) {
        this.resumeCheckoutService = resumeCheckoutService;
        this.reconcilePaymentService = reconcilePaymentService;
        this.reconcilePendingPaymentsService = reconcilePendingPaymentsService;
    }

    /**
     * Staff action: regenerate / resume a Stripe Checkout session for an order.
     * Useful when the customer lost the Checkout URL or the session expired.
     */
    @PostMapping("/orders/{orderId}/resume-checkout")
    public ResponseEntity<ResumeCheckoutResponse> resumeCheckout(@PathVariable Long orderId) {
        return ResponseEntity.ok(resumeCheckoutService.handle(orderId));
    }

    /**
     * Staff action: reconcile an order's payment by asking Stripe for the current
     * Checkout Session status and updating local Payment/Order if needed.
     */
    @PostMapping("/orders/{orderId}/reconcile")
    public ResponseEntity<ReconcilePaymentService.ReconcilePaymentResult> reconcile(@PathVariable Long orderId) {
        return ResponseEntity.ok(reconcilePaymentService.handle(orderId));
    }

    /**
     *  bulk reconcile for pending payments (demo-friendly "fix stuck orders" button).
     */
    @PostMapping("/reconcile-pending")
    public ResponseEntity<ReconcilePendingPaymentsService.BatchReconcileResult> reconcilePending(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "120") long staleSeconds
    ) {
        return ResponseEntity.ok(
                reconcilePendingPaymentsService.reconcileStalePending(limit, java.time.Duration.ofSeconds(staleSeconds))
        );
    }


}
