package com.valedosol.kaju.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.StripeSubscription;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.AccountRepository;
import com.valedosol.kaju.repository.StripeSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.public.key}")
    private String stripePublicKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final StripeSubscriptionRepository stripeSubscriptionRepository;
    private final AccountRepository accountRepository;

    public StripeService(StripeSubscriptionRepository stripeSubscriptionRepository, 
                         AccountRepository accountRepository) {
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
        this.accountRepository = accountRepository;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }

    public PaymentIntent createPaymentIntent(double amount, String currency) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount((long)(amount * 100)) // Convert to cents
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();
            
        return PaymentIntent.create(params);
    }
    // Create or get customer in Stripe
    public Customer createOrGetCustomer(Account account) throws StripeException {
        // Check if customer already exists
        StripeSubscription existingSubscription = stripeSubscriptionRepository
                .findByAccountId(account.getId())
                .orElse(null);

        if (existingSubscription != null && existingSubscription.getStripeCustomerId() != null) {
            return Customer.retrieve(existingSubscription.getStripeCustomerId());
        }

        // Create new customer using the builder pattern (SDK 29.0.0)
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(account.getEmail())
                .setName(account.getNickname())
                .build();

        Customer customer = Customer.create(params);
        
        // Create or update subscription record
        if (existingSubscription == null) {
            existingSubscription = new StripeSubscription();
            existingSubscription.setAccount(account);
        }
        
        existingSubscription.setStripeCustomerId(customer.getId());
        stripeSubscriptionRepository.save(existingSubscription);
        
        return customer;
    }

    // Create a product in Stripe (represents a subscription plan)
    public Product createProduct(SubscriptionPlan plan) throws StripeException {
        ProductCreateParams params = ProductCreateParams.builder()
            .setName(plan.getName())
            .setDescription("Plano " + plan.getName() + ": " + plan.getWeeklyAllowedSends() + " envios semanais")
            .build();
            
        return Product.create(params);
    }

    // Create a price linked to a product (defines the plan price)
    public Price createPrice(String productId, SubscriptionPlan plan) throws StripeException {
        PriceCreateParams.Recurring recurring = PriceCreateParams.Recurring.builder()
            .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
            .setIntervalCount(1L)
            .build();
            
        PriceCreateParams params = PriceCreateParams.builder()
            .setProduct(productId)
            .setUnitAmount((long)(plan.getPrice() * 100)) // Stripe works in cents
            .setCurrency("brl")
            .setRecurring(recurring)
            .build();
            
        return Price.create(params);
    }

    // Create a Stripe checkout session
    public Map<String, Object> createCheckoutSession(Account account, SubscriptionPlan plan, 
                                                   String successUrl, String cancelUrl) throws StripeException {
        // Ensure the customer exists in Stripe
        Customer customer = createOrGetCustomer(account);

        // Create product
        Product product = createProduct(plan);
        
        // Create price for the product
        Price price = createPrice(product.getId(), plan);
        
        // Create checkout session with builder pattern
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        lineItems.add(
            SessionCreateParams.LineItem.builder()
                .setPrice(price.getId())
                .setQuantity(1L)
                .build()
        );
        
        SessionCreateParams params = SessionCreateParams.builder()
            .setCustomer(customer.getId())
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .addAllLineItem(lineItems)
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .build();
        
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
        
        // Return data needed for the frontend
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sessionId", session.getId());
        responseData.put("publicKey", stripePublicKey);
        
        return responseData;
    }

    // Process Stripe webhook events
    public void processWebhookEvent(String payload, String sigHeader) throws StripeException {
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new StripeException("Invalid signature", null, null, null, null) {};
        }
        
        // Process the event based on type
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        
        switch (event.getType()) {
            case "checkout.session.completed":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    com.stripe.model.checkout.Session session = 
                        (com.stripe.model.checkout.Session) dataObjectDeserializer.getObject().get();
                    handleSuccessfulPayment(session);
                }
                break;
                
            case "invoice.paid":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    Invoice invoice = (Invoice) dataObjectDeserializer.getObject().get();
                    handleSuccessfulRenewal(invoice);
                }
                break;
                
            case "customer.subscription.updated":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    com.stripe.model.Subscription subscription = 
                        (com.stripe.model.Subscription) dataObjectDeserializer.getObject().get();
                    handleSubscriptionUpdate(subscription);
                }
                break;
                
            case "customer.subscription.deleted":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    com.stripe.model.Subscription canceledSubscription = 
                        (com.stripe.model.Subscription) dataObjectDeserializer.getObject().get();
                    handleSubscriptionCancellation(canceledSubscription);
                }
                break;
                
            default:
                System.out.println("Unhandled event type: " + event.getType());
        }
    }

    private void handleSuccessfulPayment(com.stripe.model.checkout.Session session) {
        // Activate user subscription
        String subscriptionId = session.getSubscription();
        try {
            activateSubscription(subscriptionId);
        } catch (StripeException e) {
            e.printStackTrace();
        }
    }

    private void handleSuccessfulRenewal(Invoice invoice) {
        // Process subscription renewal
        String subscriptionId = invoice.getSubscription();
        try {
            activateSubscription(subscriptionId);
        } catch (StripeException e) {
            e.printStackTrace();
        }
    }

    private void handleSubscriptionUpdate(com.stripe.model.Subscription subscription) {
        // Update subscription status in the system
        // Example: plan change, pause, etc.
    }

    private void handleSubscriptionCancellation(com.stripe.model.Subscription subscription) {
        // Mark subscription as canceled in the system
        StripeSubscription stripeSubscription = stripeSubscriptionRepository
            .findByStripeSubscriptionId(subscription.getId())
            .orElse(null);
        
        if (stripeSubscription != null) {
            stripeSubscription.setStatus("canceled");
            stripeSubscriptionRepository.save(stripeSubscription);
            
            // Update user plan to free tier
            Account account = stripeSubscription.getAccount();
            account.setSubscriptionPlan(null);
            accountRepository.save(account);
        }
    }

    // Activate subscription after successful payment
    public void activateSubscription(String stripeSubscriptionId) throws StripeException {
        com.stripe.model.Subscription stripeSubscription = 
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        
        StripeSubscription subscription = stripeSubscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (subscription != null && "active".equals(stripeSubscription.getStatus())) {
            // Update subscription status
            subscription.setStatus("active");
            
            // Update next billing date
            long nextBillingTimestamp = stripeSubscription.getCurrentPeriodEnd();
            LocalDateTime nextBillingDate = 
                LocalDateTime.ofInstant(Instant.ofEpochSecond(nextBillingTimestamp), ZoneId.systemDefault());
            subscription.setNextBillingDate(nextBillingDate);
            
            stripeSubscriptionRepository.save(subscription);
            
            // Update user plan in the system
            Account account = subscription.getAccount();
            account.setSubscriptionPlan(subscription.getSubscriptionPlan());
            account.setRemainingWeeklySends(subscription.getSubscriptionPlan().getWeeklyAllowedSends());
            accountRepository.save(account);
        }
    }

    // Cancel a subscription
    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        SubscriptionCancelParams params = SubscriptionCancelParams.builder()
            .build();
            
        com.stripe.model.Subscription subscription = 
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        subscription.cancel(params);
        
        // Update subscription status in the database
        StripeSubscription stripeSubscription = stripeSubscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (stripeSubscription != null) {
            stripeSubscription.setStatus("canceled");
            stripeSubscriptionRepository.save(stripeSubscription);
        }
    }
}