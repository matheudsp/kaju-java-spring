package com.valedosol.kaju.controller.Stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.StripeSubscription;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.AccountRepository;
import com.valedosol.kaju.repository.StripeSubscriptionRepository;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;
import com.valedosol.kaju.service.Stripe.StripeService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v2/stripe")
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final AccountRepository accountRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StripeSubscriptionRepository stripeSubscriptionRepository;
    private final String webhookSecret;

    @Autowired
    public StripeWebhookController(
            StripeService stripeService,
            AccountRepository accountRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            StripeSubscriptionRepository stripeSubscriptionRepository,
            @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.stripeService = stripeService;
        this.accountRepository = accountRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Received webhook request with signature: {}", sigHeader.substring(0, 10) + "...");

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid signature", e);
            return ResponseEntity.badRequest().body("Invalid webhook signature");
        }

        // Handle the event
        switch (event.getType()) {
            case "customer.subscription.created":
            case "customer.subscription.updated":
                handleSubscriptionUpdate(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionCancelled(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            default:
                log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook received");
    }

    private void handleSubscriptionUpdate(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = subscription.getId();
            String customerId = subscription.getCustomer();
            String status = subscription.getStatus();

            // Find user by customer ID
            String userId = subscription.getMetadata().get("userId");
            if (userId == null) {
                log.error("Missing userId in subscription metadata");
                return;
            }

            Optional<Account> accountOpt = accountRepository.findById(Long.valueOf(userId));

            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();

                // Find subscription plan by ID from metadata
                String planId = subscription.getMetadata().get("planId");
                if (planId == null) {
                    log.error("Missing planId in subscription metadata");
                    return;
                }

                Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(Long.valueOf(planId));

                if (planOpt.isPresent()) {
                    SubscriptionPlan plan = planOpt.get();

                    // Update or create stripe subscription
                    Optional<StripeSubscription> existingSubOpt = stripeSubscriptionRepository
                            .findByStripeSubscriptionId(subscriptionId);

                    StripeSubscription stripeSubscription;
                    if (existingSubOpt.isPresent()) {
                        stripeSubscription = existingSubOpt.get();
                    } else {
                        stripeSubscription = new StripeSubscription();
                        stripeSubscription.setAccount(account);
                        stripeSubscription.setSubscriptionPlan(plan);
                        stripeSubscription.setStripeCustomerId(customerId);
                        stripeSubscription.setStripeSubscriptionId(subscriptionId);
                    }

                    stripeSubscription.setStatus(status);

                    // Handle subscription statuses
                    if ("active".equals(status)) {
                        // Activate subscription benefits
                        account.setSubscriptionPlan(plan);
                        account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
                        accountRepository.save(account);
                    }

                    stripeSubscriptionRepository.save(stripeSubscription);
                }
            }
        } catch (Exception e) {
            log.error("Error processing subscription update", e);
        }
    }

    private void handleSubscriptionCancelled(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = subscription.getId();

            Optional<StripeSubscription> stripeSubOpt = stripeSubscriptionRepository
                    .findByStripeSubscriptionId(subscriptionId);

            if (stripeSubOpt.isPresent()) {
                StripeSubscription stripeSub = stripeSubOpt.get();
                stripeSub.setStatus("canceled");
                stripeSubscriptionRepository.save(stripeSub);

                // Optional: Handle account changes when subscription is cancelled
                // Account account = stripeSub.getAccount();
                // You may want to keep the subscription until the end of the billing period
                // or immediately remove benefits depending on your business logic
            }
        } catch (Exception e) {
            log.error("Error processing subscription cancellation", e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        // Handle successful payment
        log.info("Payment succeeded: {}", event.getId());
    }

    private void handleInvoicePaymentFailed(Event event) {
        // Handle failed payment
        log.info("Payment failed: {}", event.getId());
        // You might want to notify the user or take other actions
    }
}