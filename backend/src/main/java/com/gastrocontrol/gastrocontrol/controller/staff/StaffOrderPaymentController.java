package com.gastrocontrol.gastrocontrol.controller.staff;

import com.gastrocontrol.gastrocontrol.application.service.payment.GetOrderPaymentService;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderPaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/orders")
public class StaffOrderPaymentController {

    private final GetOrderPaymentService getOrderPaymentService;

    public StaffOrderPaymentController(GetOrderPaymentService getOrderPaymentService) {
        this.getOrderPaymentService = getOrderPaymentService;
    }

    @GetMapping("/{orderId}/payment")
    public ResponseEntity<OrderPaymentResponse> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(getOrderPaymentService.handle(orderId));
    }
}
