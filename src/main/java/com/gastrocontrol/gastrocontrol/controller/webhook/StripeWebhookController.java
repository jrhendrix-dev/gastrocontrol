package com.gastrocontrol.gastrocontrol.controller.webhook;

import com.gastrocontrol.gastrocontrol.application.service.payment.HandleStripeCheckoutWebhookService;
import com.gastrocontrol.gastrocontrol.config.StripeProperties;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private final StripeProperties props;
    private final HandleStripeCheckoutWebhookService webhookService;

    public StripeWebhookController(StripeProperties props, HandleStripeCheckoutWebhookService webhookService) {
        this.props = props;
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handle(@RequestHeader("Stripe-Signature") String sigHeader,
                                       HttpServletRequest request) {
        String payload;
        try {
            payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Stripe webhook: failed to read request body", e);
            return ResponseEntity.ok().build(); // ACK to avoid retries
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, props.webhookSecret());
        } catch (SignatureVerificationException e) {
            // Only case where 400 is correct
            throw new ValidationException(Map.of("stripeSignature", "Invalid Stripe signature"));
        }

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new IllegalStateException("Unable to deserialize session"));

                String sessionId = session.getId();
                String paymentStatus = session.getPaymentStatus();
                String paymentIntentId = session.getPaymentIntent();

                Long orderId = null;

                // client_reference_id MUST be numeric if you parse it
                String ref = session.getClientReferenceId();
                if (ref != null && !ref.isBlank()) {
                    try {
                        orderId = Long.valueOf(ref);
                    } catch (NumberFormatException nfe) {
                        log.warn("Stripe webhook: client_reference_id is not numeric: {}", ref);
                    }
                }

                if (orderId == null && session.getMetadata() != null) {
                    String metaOrderId = session.getMetadata().get("orderId");
                    if (metaOrderId != null && !metaOrderId.isBlank()) {
                        orderId = Long.valueOf(metaOrderId);
                    }
                }

                if (orderId == null) {
                    log.warn("Stripe webhook: Missing orderId for session {} (ref/metadata)", sessionId);
                    return ResponseEntity.ok().build(); // ACK; you can't process
                }

                webhookService.handleCheckoutSessionCompleted(sessionId, paymentStatus, orderId, paymentIntentId);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Don't 400 -> Stripe retries forever.
            log.error("Stripe webhook processing failed (eventType={}, id={})",
                    event.getType(), event.getId(), e);
            return ResponseEntity.ok().build();
        }
    }
}
