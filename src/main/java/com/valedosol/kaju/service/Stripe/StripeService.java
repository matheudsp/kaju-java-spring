package com.valedosol.kaju.service.Stripe;

import com.valedosol.kaju.dto.*;
import com.valedosol.kaju.model.SubscriptionPlan;


import com.valedosol.kaju.repository.SubscriptionPlanRepository;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class StripeService {
        private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
public StripeService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }
    @PostConstruct
    public void init() {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            log.warn("Stripe API key not configured. Stripe functionality will not work.");
            // Provide some fallback behavior or just log a warning
        } else {
            Stripe.apiKey = stripeApiKey;
        }
    }
   

    public SessionDto createPaymentSession(SessionDto sessionDto) {

        try {
            double amount = 23.00; // 2300

            Customer customer = findOrCreateCustomer("test@gmail.com", "Test User");

            String clientUrl = "https://localhost:4200";
            SessionCreateParams.Builder sessionCreateParamsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientUrl + "/failure");

            // add item and amount
            sessionCreateParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData
                                    .builder()
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .putMetadata("cart_id", "123")
                                            .putMetadata("user_id", sessionDto.getUserId())
                                            .setName("Shoes XL")
                                            .build())
                                    .setCurrency("USD")
                                    .setUnitAmountDecimal(BigDecimal.valueOf(amount * 100))
                                    .build())
                            .build())
                    .build();

            SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("cart_id", "123")
                    .putMetadata("user_id", sessionDto.getUserId())
                    .build();
            sessionCreateParamsBuilder.setPaymentIntentData(paymentIntentData);
            Session session = Session.create(sessionCreateParamsBuilder.build());
            sessionDto.setSessionUrl(session.getUrl());
            sessionDto.setSessionId(session.getId());
        } catch (StripeException e) {

            log.error("Exception createPaymentSession", e);
            sessionDto.setMessage(e.getMessage());
        }

        return sessionDto;
    }

    private Customer findOrCreateCustomer(String email, String fullName) throws StripeException {

        CustomerSearchParams params = CustomerSearchParams.builder()
                .setQuery("email:'" + email + "'")
                .build();

        CustomerSearchResult search = Customer.search(params);
        Customer customer;
        if (search.getData().isEmpty()) {

            CustomerCreateParams customerCreateParams = CustomerCreateParams.builder()
                    .setName(fullName)
                    .setEmail(email)
                    .build();
            customer = Customer.create(customerCreateParams);
        } else {
            customer = search.getData().get(0);
        }

        return customer;
    }

    public SessionDto createSubscriptionSession(SessionDto sessionDto) {
    try {
        // Get subscription plan from repository instead of hardcoded values
        Long planId = Long.valueOf(sessionDto.getData().get("planId"));
        Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(planId);
        
        if (!planOpt.isPresent()) {
            sessionDto.setMessage("Subscription plan not found");
            return sessionDto;
        }
        
        SubscriptionPlan plan = planOpt.get();
        String email = sessionDto.getData().get("email");
        String fullName = sessionDto.getData().get("fullName");
        
        // Find or create customer
        Customer customer = findOrCreateCustomer(email, fullName);
        
        String clientUrl = "https://yourfrontendurl.com";
        SessionCreateParams.Builder sessionCreateParamsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customer.getId())
                .setSuccessUrl(clientUrl + "/success-subscription?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(clientUrl + "/failure");
        
        // Use Stripe Price ID from the plan
        sessionCreateParamsBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(plan.getStripePriceId())
                        .build());
        
        // Add metadata
        SessionCreateParams.SubscriptionData subscriptionData = SessionCreateParams.SubscriptionData.builder()
                .putMetadata("planId", plan.getId().toString())
                .putMetadata("userId", sessionDto.getUserId())
                .build();
        
        sessionCreateParamsBuilder.setSubscriptionData(subscriptionData);
        Session session = Session.create(sessionCreateParamsBuilder.build());
        
        sessionDto.setSessionUrl(session.getUrl());
        sessionDto.setSessionId(session.getId());
    } catch (StripeException e) {
        log.error("Exception creating subscription session", e);
        sessionDto.setMessage(e.getMessage());
    }
    
    return sessionDto;
}



}
