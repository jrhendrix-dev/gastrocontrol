// src/main/java/com/gastrocontrol/gastrocontrol/controller/customer/CustomerCheckoutController.java
package com.gastrocontrol.gastrocontrol.controller.customer;

import com.gastrocontrol.gastrocontrol.application.service.customer.CustomerCashCheckoutService;
import com.gastrocontrol.gastrocontrol.application.service.customer.StartCustomerCheckoutService;
import com.gastrocontrol.gastrocontrol.config.StripeProperties;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerCheckoutRequest;
import com.gastrocontrol.gastrocontrol.dto.customer.CustomerCheckoutResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing order placement endpoints.
 *
 * <p>Two checkout paths:</p>
 * <ul>
 *   <li>{@code POST /checkout} — Stripe payment: creates order + Stripe session,
 *       returns redirect URL.</li>
 *   <li>{@code POST /cash-checkout} — Cash payment: creates order, submits to kitchen,
 *       records pending MANUAL payment. No redirect.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/customer/orders")
public class CustomerCheckoutController {

    private final StartCustomerCheckoutService stripeCheckoutService;
    private final CustomerCashCheckoutService  cashCheckoutService;
    private final StripeProperties             stripeProps;

    public CustomerCheckoutController(
            StartCustomerCheckoutService stripeCheckoutService,
            CustomerCashCheckoutService cashCheckoutService,
            StripeProperties stripeProps
    ) {
        this.stripeCheckoutService = stripeCheckoutService;
        this.cashCheckoutService   = cashCheckoutService;
        this.stripeProps           = stripeProps;
    }

    /**
     * Stripe checkout — creates order and Stripe Checkout session.
     * Frontend should redirect to the returned {@code checkoutUrl}.
     *
     * @param req order type, customer details, and cart items
     * @return 201 with orderId and Stripe Checkout redirect URL
     */
    @PostMapping("/checkout")
    public ResponseEntity<CustomerCheckoutResponse> checkout(
            @Valid @RequestBody CustomerCheckoutRequest req
    ) {
        String currency = stripeProps.currency() == null ? "eur" : stripeProps.currency();
        var result = stripeCheckoutService.start(req, currency);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomerCheckoutResponse(
            result.orderId(),
            result.checkoutUrl(),
            result.trackingToken()
        ));
    }

    /**
     * Cash checkout — creates order, submits to kitchen immediately, and records
     * a pending MANUAL payment. Staff confirms cash receipt at pickup or delivery.
     *
     * <p>No Stripe session is created. The frontend navigates directly to the
     * confirmation page after this call.</p>
     *
     * @param req order type, customer details, and cart items
     * @return 201 with orderId (no checkoutUrl — navigation is handled by the frontend)
     */
    @PostMapping("/cash-checkout")
    public ResponseEntity<CashCheckoutResponse> cashCheckout(
            @Valid @RequestBody CustomerCheckoutRequest req
    ) {
        var result = cashCheckoutService.handle(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CashCheckoutResponse(result.orderId(), result.trackingToken()));
    }

    /** Response body for the cash checkout endpoint. */
    /**
     * Response body for the cash checkout endpoint.
     * trackingToken is the opaque UUID for /track/{token}
     */
    public record CashCheckoutResponse(long orderId, String trackingToken) {}
}