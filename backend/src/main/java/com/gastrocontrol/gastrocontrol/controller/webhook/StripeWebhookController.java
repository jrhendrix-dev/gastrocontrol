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
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
            HttpServletRequest request
    ) {
        try {
            String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            Event event;

            if (props.verifyWebhookSignature()) {
                event = Webhook.constructEvent(payload, sigHeader, props.webhookSecret());
            } else {
                // Demo / dev mode — trust Stripe CLI
                event = Event.GSON.fromJson(payload, Event.class);
            }

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new ValidationException(
                                Map.of("stripe", "Unable to deserialize session")
                        ));

                webhookService.handleCheckoutSessionCompleted(
                        session.getId(),
                        session.getPaymentStatus(),
                        Long.valueOf(session.getClientReferenceId()),
                        session.getPaymentIntent()
                );
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stripe webhook processing failed", e);
            throw new ValidationException(Map.of("stripe", "Webhook processing failed"));
        }
    }

}
