package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.application.service.payment.ResumeCheckoutService;
import com.gastrocontrol.gastrocontrol.dto.staff.ResumeCheckoutResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/payments")
public class StaffPaymentController {

    private final ResumeCheckoutService resumeCheckoutService;

    public StaffPaymentController(ResumeCheckoutService resumeCheckoutService) {
        this.resumeCheckoutService = resumeCheckoutService;
    }

    /**
     * Staff action: regenerate / resume a Stripe Checkout session for an order.
     * Useful when the customer lost the Checkout URL or the session expired.
     */
    @PostMapping("/orders/{orderId}/resume-checkout")
    public ResponseEntity<ResumeCheckoutResponse> resumeCheckout(@PathVariable Long orderId) {
        return ResponseEntity.ok(resumeCheckoutService.handle(orderId));
    }
}
