package com.valedosol.kaju.feature.subscription.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.valedosol.kaju.feature.subscription.service.StripeWebhookService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to handle Stripe webhook events
 */
@RestController
@RequestMapping("/api/v2/stripe")
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService webhookService;
    private final String webhookSecret;

    public StripeWebhookController(
            StripeWebhookService webhookService,
            @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.webhookService = webhookService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        log.info("Received webhook request with signature: {}", 
                sigHeader.length() > 10 ? sigHeader.substring(0, 10) + "..." : sigHeader);

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid webhook signature");
        }

        log.info("Processing Stripe event: {}", event.getType());

        // Handle the event based on type
        switch (event.getType()) {
            case "customer.subscription.created", "customer.subscription.updated" -> 
                webhookService.handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> 
                webhookService.handleSubscriptionDeleted(event);
            case "invoice.payment_succeeded" -> 
                webhookService.handlePaymentSucceeded(event);
            case "invoice.payment_failed" -> 
                webhookService.handlePaymentFailed(event);
            default -> 
                log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processed successfully");
    }
}