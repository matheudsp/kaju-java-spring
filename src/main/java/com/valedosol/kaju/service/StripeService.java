package com.valedosol.kaju.service;

import com.valedosol.kaju.dto.*;

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

    @Value("${stripe.api.key}")
    private String stripeApiKey;

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

            Customer customer = findOrCreateCustomer("test@gmail.com", "Test User");

            String clientUrl = "https://localhost:4200";
            SessionCreateParams.Builder sessionCreateParamsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientUrl + "/success-subscription?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientUrl + "/failure")
                    // trial
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays(30L)
                            .build());

            String aPackage = String.valueOf(sessionDto.getData().get("PACKAGE"));
            // add item and amount
            sessionCreateParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData
                                    .builder()
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .putMetadata("package", aPackage)
                                            .putMetadata("user_id", sessionDto.getUserId())
                                            .setName(aPackage)
                                            .build())
                                    .setCurrency("USD")
                                    .setUnitAmountDecimal(BigDecimal
                                            .valueOf(Objects.equals(aPackage, "YEAR") ? 99.99 * 100 : 9.99 * 100))
                                    // recurring
                                    .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                            .setInterval(Objects.equals(aPackage, "YEAR")
                                                    ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                                                    : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SessionCreateParams.SubscriptionData subscriptionData = SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("package", aPackage)
                    .putMetadata("user_id", sessionDto.getUserId())
                    .build();
            sessionCreateParamsBuilder.setSubscriptionData(subscriptionData);
            Session session = Session.create(sessionCreateParamsBuilder.build());
            sessionDto.setSessionUrl(session.getUrl());
            sessionDto.setSessionId(session.getId());
        } catch (StripeException e) {

            log.error("Exception createPaymentSession", e);
            sessionDto.setMessage(e.getMessage());
        }

        return sessionDto;
    }

}
