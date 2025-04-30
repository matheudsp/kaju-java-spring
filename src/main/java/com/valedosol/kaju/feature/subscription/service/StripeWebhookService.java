package com.valedosol.kaju.feature.subscription.service;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.subscription.model.StripeSubscription;
import com.valedosol.kaju.feature.subscription.repository.StripeSubscriptionRepository;
import com.valedosol.kaju.feature.subscription.model.SubscriptionPlan;
import com.valedosol.kaju.feature.subscription.repository.SubscriptionPlanRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to handle Stripe webhook events
 */
@Service
@Slf4j
public class StripeWebhookService {

    private final AccountRepository accountRepository;
    private final SubscriptionPlanRepository planRepository;
    private final StripeSubscriptionRepository subscriptionRepository;

    public StripeWebhookService(
            AccountRepository accountRepository,
            SubscriptionPlanRepository planRepository,
            StripeSubscriptionRepository subscriptionRepository) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Handle subscription created or updated events
     */
    @Transactional
    public void handleSubscriptionUpdated(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            
            // Extract metadata
            String subscriptionId = subscription.getId();
            String customerId = subscription.getCustomer();
            String status = subscription.getStatus();
            
            String userId = subscription.getMetadata().get("userId");
            String planId = subscription.getMetadata().get("planId");
            
            if (userId == null || planId == null) {
                log.error("Missing required metadata in subscription. userId: {}, planId: {}", userId, planId);
                return;
            }

            // Get account and plan
            Optional<Account> accountOpt = accountRepository.findById(Long.valueOf(userId));
            Optional<SubscriptionPlan> planOpt = planRepository.findById(Long.valueOf(planId));

            if (accountOpt.isEmpty() || planOpt.isEmpty()) {
                log.error("Could not find account or plan for subscription. accountId: {}, planId: {}", userId, planId);
                return;
            }

            Account account = accountOpt.get();
            SubscriptionPlan plan = planOpt.get();

            // Create or update subscription record
            Optional<StripeSubscription> existingSubOpt = subscriptionRepository
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
            subscriptionRepository.save(stripeSubscription);

            // Update account if subscription is active
            if ("active".equals(status)) {
                account.setSubscriptionPlan(plan);
                account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
                accountRepository.save(account);
                log.info("Activated subscription for user: {}, plan: {}", account.getEmail(), plan.getName());
            }
        } catch (Exception e) {
            log.error("Error processing subscription update", e);
        }
    }

    /**
     * Handle subscription deleted events
     */
    @Transactional
    public void handleSubscriptionDeleted(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = subscription.getId();

            Optional<StripeSubscription> stripeSubOpt = subscriptionRepository
                    .findByStripeSubscriptionId(subscriptionId);

            if (stripeSubOpt.isPresent()) {
                StripeSubscription stripeSub = stripeSubOpt.get();
                stripeSub.setStatus("canceled");
                subscriptionRepository.save(stripeSub);
                log.info("Marked subscription as canceled: {}", subscriptionId);
                
                // Note: You might want to handle account changes when subscription is canceled
                // depending on your business logic
            }
        } catch (Exception e) {
            log.error("Error processing subscription cancellation", e);
        }
    }

    /**
     * Handle payment succeeded events
     */
    public void handlePaymentSucceeded(Event event) {
        log.info("Payment succeeded: {}", event.getId());
        // Implement additional handling if needed
    }

    /**
     * Handle payment failed events
     */
    public void handlePaymentFailed(Event event) {
        log.info("Payment failed: {}", event.getId());
        // Implement notification or other actions for failed payments
    }
}