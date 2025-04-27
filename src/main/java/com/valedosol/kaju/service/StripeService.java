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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;


@Service
@Slf4j
public class StripeService {

    @Value("${api.stripe.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {

        Stripe.apiKey = stripeApiKey;
    }

    public StripeTokenDto createCardToken(StripeTokenDto model) {

        try {
            Map<String, Object> card = new HashMap<>();
            card.put("number", model.getCardNumber());
            card.put("exp_month", Integer.parseInt(model.getExpMonth()));
            card.put("exp_year", Integer.parseInt(model.getExpYear()));
            card.put("cvc", model.getCvc());
            Map<String, Object> params = new HashMap<>();
            params.put("card", card);
            Token token = Token.create(params);
            if (token != null && token.getId() != null) {
                model.setSuccess(true);
                model.setToken(token.getId());
            }
            return model;
        } catch (StripeException e) {
            log.error("StripeService (createCardToken)", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    public StripeChargeDto charge(StripeChargeDto chargeRequest) {


        try {
            chargeRequest.setSuccess(false);
            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", (int) (chargeRequest.getAmount() * 100));
            chargeParams.put("currency", "USD");
            chargeParams.put("description", "Payment for id " + chargeRequest.getAdditionalInfo().getOrDefault("ID_TAG", ""));
            chargeParams.put("source", chargeRequest.getStripeToken());
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("id", chargeRequest.getChargeId());
            metaData.putAll(chargeRequest.getAdditionalInfo());
            chargeParams.put("metadata", metaData);
            Charge charge = Charge.create(chargeParams);
            chargeRequest.setMessage(charge.getOutcome().getSellerMessage());

            if (charge.getPaid()) {
                chargeRequest.setChargeId(charge.getId());
                chargeRequest.setSuccess(true);

            }
            return chargeRequest;
        } catch (StripeException e) {
            log.error("StripeService (charge)", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    public StripeSubscriptionResponse createSubscription(StripeSubscriptionDto subscriptionDto){


        PaymentMethod paymentMethod = createPaymentMethod(subscriptionDto);
        Customer customer = createCustomer(paymentMethod, subscriptionDto);
        paymentMethod = attachCustomerToPaymentMethod(customer, paymentMethod);
        Subscription subscription = createSubscription(subscriptionDto, paymentMethod, customer);

        return createResponse(subscriptionDto,paymentMethod,customer,subscription);
    }

    private StripeSubscriptionResponse createResponse(StripeSubscriptionDto subscriptionDto, PaymentMethod paymentMethod, Customer customer, Subscription subscription) {



        return StripeSubscriptionResponse.builder()
                .username(subscriptionDto.getUsername())
                .stripePaymentMethodId(paymentMethod.getId())
                .stripeSubscriptionId(subscription.getId())
                .stripeCustomerId(customer.getId())
                .build();
    }

    private PaymentMethod createPaymentMethod(StripeSubscriptionDto subscriptionDto){

        try {

            Map<String, Object> card = new HashMap<>();

            card.put("number", subscriptionDto.getCardNumber());
            card.put("exp_month", Integer.parseInt(subscriptionDto.getExpMonth()));
            card.put("exp_year", Integer.parseInt(subscriptionDto.getExpYear()));
            card.put("cvc", subscriptionDto.getCvc());

            Map<String, Object> params = new HashMap<>();
            params.put("type", "card");
            params.put("card", card);

            return PaymentMethod.create(params);

        } catch (StripeException e) {
            log.error("StripeService (createPaymentMethod)", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private Customer createCustomer(PaymentMethod paymentMethod,StripeSubscriptionDto subscriptionDto){

        try {

            Map<String, Object> customerMap = new HashMap<>();
            customerMap.put("name", subscriptionDto.getUsername());
            customerMap.put("email", subscriptionDto.getEmail());
            customerMap.put("payment_method", paymentMethod.getId());

            return Customer.create(customerMap);
        } catch (StripeException e) {
            log.error("StripeService (createCustomer)", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    private PaymentMethod attachCustomerToPaymentMethod(Customer customer,PaymentMethod paymentMethod){

        try {

            paymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethod.getId());

            Map<String, Object> params = new HashMap<>();
            params.put("customer", customer.getId());
            paymentMethod = paymentMethod.attach(params);
            return paymentMethod;


        } catch (StripeException e) {
            log.error("StripeService (attachCustomerToPaymentMethod)", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    private Subscription createSubscription(StripeSubscriptionDto subscriptionDto,PaymentMethod paymentMethod,Customer customer){

        try {

            List<Object> items = new ArrayList<>();
            Map<String, Object> item1 = new HashMap<>();
            item1.put(
                    "price",
                    subscriptionDto.getPriceId()
            );
            item1.put("quantity",subscriptionDto.getNumberOfLicense());
            items.add(item1);

            Map<String, Object> params = new HashMap<>();
            params.put("customer", customer.getId());
            params.put("default_payment_method", paymentMethod.getId());
            params.put("items", items);
            return Subscription.create(params);
        } catch (StripeException e) {
            log.error("StripeService (createSubscription)", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    public  Subscription cancelSubscription(String subscriptionId){

        try {
            Subscription retrieve = Subscription.retrieve(subscriptionId);
            return retrieve.cancel();
        } catch (StripeException e) {

            log.error("StripeService (cancelSubscription)",e);
        }

        return null;
    }


    public SessionDto createPaymentSession(SessionDto sessionDto){

        try {
            double amount = 23.00; // 2300

            Customer customer = findOrCreateCustomer("test@gmail.com","Test User");

            String clientUrl ="https://localhost:4200";
            SessionCreateParams.Builder sessionCreateParamsBuilder =SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientUrl+"/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientUrl+"/failure");

            // add item and amount
            sessionCreateParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData
                                    .builder()
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .putMetadata("cart_id","123")
                                            .putMetadata("user_id",sessionDto.getUserId())
                                            .setName("Shoes XL")
                                            .build()
                                    )
                                    .setCurrency("USD")
                                    .setUnitAmountDecimal(BigDecimal.valueOf(amount * 100))
                                    .build())
                            .build()
            ).build();

            SessionCreateParams.PaymentIntentData paymentIntentData=
                    SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata("cart_id","123")
                            .putMetadata("user_id",sessionDto.getUserId())
                            .build();
            sessionCreateParamsBuilder.setPaymentIntentData(paymentIntentData);
            Session session = Session.create(sessionCreateParamsBuilder.build());
            sessionDto.setSessionUrl(session.getUrl());
            sessionDto.setSessionId(session.getId());
        }catch (StripeException e){

            log.error("Exception createPaymentSession",e);
            sessionDto.setMessage(e.getMessage());
        }


        return sessionDto;
    }

    private Customer findOrCreateCustomer(String email, String fullName) throws StripeException {

        CustomerSearchParams params=
                CustomerSearchParams.builder()
                        .setQuery("email:'"+email+"'")
                        .build();

        CustomerSearchResult search = Customer.search(params);
        Customer customer;
        if(search.getData().isEmpty()){

            CustomerCreateParams customerCreateParams=
                    CustomerCreateParams.builder()
                            .setName(fullName)
                            .setEmail(email)
                            .build();
            customer = Customer.create(customerCreateParams);
        }else {
            customer = search.getData().get(0);
        }

        return customer;
    }



    public SessionDto createSubscriptionSession(SessionDto sessionDto){

        try {

            Customer customer = findOrCreateCustomer("test@gmail.com","Test User");

            String clientUrl ="https://localhost:4200";
            SessionCreateParams.Builder sessionCreateParamsBuilder =SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(clientUrl+"/success-subscription?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(clientUrl+"/failure")
                    // trial
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays(30L)
                            .build())
                    ;


            String aPackage = String.valueOf(sessionDto.getData().get("PACKAGE"));
            // add item and amount
            sessionCreateParamsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData
                                    .builder()
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .putMetadata("package",aPackage)
                                            .putMetadata("user_id",sessionDto.getUserId())
                                            .setName(aPackage)
                                            .build()
                                    )
                                    .setCurrency("USD")
                                    .setUnitAmountDecimal(BigDecimal.valueOf(Objects.equals(aPackage,"YEAR")?99.99 * 100:9.99*100))
                                   //recurring
                                    .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                            .setInterval(Objects.equals(aPackage,"YEAR")?SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR: SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                            .build())
                                    .build())
                            .build()
            ).build();

            SessionCreateParams.SubscriptionData subscriptionData=
                    SessionCreateParams.SubscriptionData.builder()
                             .putMetadata("package",aPackage)
                    .putMetadata("user_id",sessionDto.getUserId())
                            .build();
            sessionCreateParamsBuilder.setSubscriptionData(subscriptionData);
            Session session = Session.create(sessionCreateParamsBuilder.build());
            sessionDto.setSessionUrl(session.getUrl());
            sessionDto.setSessionId(session.getId());
        }catch (StripeException e){

            log.error("Exception createPaymentSession",e);
            sessionDto.setMessage(e.getMessage());
        }


        return sessionDto;
    }


}

