package com.valedosol.kaju.feature.subscriptionPayment.service;


import com.valedosol.kaju.feature.subscriptionPayment.dto.SessionDto;
import com.valedosol.kaju.feature.subscriptionPlan.model.SubscriptionPlan;
import com.valedosol.kaju.feature.subscriptionPlan.repository.SubscriptionPlanRepository;
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

import java.util.*;

@Service
@Slf4j
public class StripeService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${frontend.client.url}")
    private String clientUrl;

    public StripeService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @PostConstruct
    public void init() {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            log.warn("Stripe API key not configured. Stripe functionality will not work.");
        } else {
            Stripe.apiKey = stripeApiKey;
        }
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
            // Get subscription plan from repository
            Long planId = Long.valueOf(sessionDto.getData().get("planId"));
            Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(planId);

            if (!planOpt.isPresent()) {
                sessionDto.setMessage("Subscription plan not found");
                return sessionDto;
            }

            SubscriptionPlan plan = planOpt.get();
            String email = sessionDto.getData().get("email");
            String fullName = sessionDto.getData().get("fullName");

            // Ensure the plan has a price ID in Stripe
            if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
                sessionDto.setMessage("This plan is not properly configured with Stripe");
                return sessionDto;
            }

            // Find or create customer
            Customer customer = findOrCreateCustomer(email, fullName);

            SessionCreateParams.Builder sessionParamsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientUrl + "/success-subscription?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientUrl + "/failure");

            // Add the subscription line item using the plan's price ID
            sessionParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPrice(plan.getStripePriceId())
                            .build());

            // Add metadata to the subscription
            SessionCreateParams.SubscriptionData subscriptionData = SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("planId", plan.getId().toString())
                    .putMetadata("userId", sessionDto.getUserId())
                    .build();

            sessionParamsBuilder.setSubscriptionData(subscriptionData);
            Session session = Session.create(sessionParamsBuilder.build());

            sessionDto.setSessionUrl(session.getUrl());
            sessionDto.setSessionId(session.getId());
        } catch (StripeException e) {
            log.error("Exception creating subscription session", e);
            sessionDto.setMessage(e.getMessage());
        }

        return sessionDto;
    }
}