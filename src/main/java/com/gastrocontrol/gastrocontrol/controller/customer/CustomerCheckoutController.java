package com.gastrocontrol.gastrocontrol.controller.customer;

import com.gastrocontrol.gastrocontrol.application.service.customer.StartCustomerCheckoutService;
import com.gastrocontrol.gastrocontrol.config.StripeProperties;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerCheckoutRequest;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerCheckoutResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerCheckoutController {

    private final StartCustomerCheckoutService checkoutService;
    private final StripeProperties stripeProps;

    public CustomerCheckoutController(StartCustomerCheckoutService checkoutService, StripeProperties stripeProps) {
        this.checkoutService = checkoutService;
        this.stripeProps = stripeProps;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CustomerCheckoutResponse> checkout(@Valid @RequestBody CustomerCheckoutRequest req) {
        var result = checkoutService.start(req, stripeProps.currency() == null ? "eur" : stripeProps.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CustomerCheckoutResponse(result.orderId(), result.checkoutUrl()));
    }
}
