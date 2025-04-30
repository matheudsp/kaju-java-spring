package com.valedosol.kaju.feature.subscription.service;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.subscription.dto.PaymentSessionResponse;
import com.valedosol.kaju.feature.subscription.dto.SubscriptionResponse;
import com.valedosol.kaju.feature.subscription.dto.SubscriptionException;
import com.valedosol.kaju.feature.subscription.dto.SessionDto;
import com.valedosol.kaju.feature.subscription.model.SubscriptionPlan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SubscriptionService {

    private final AccountRepository accountRepository;
    private final SubscriptionPlanService planService;
    private final StripeService stripeService;

    public SubscriptionService(
            AccountRepository accountRepository,
            SubscriptionPlanService planService,
            StripeService stripeService) {
        this.accountRepository = accountRepository;
        this.planService = planService;
        this.stripeService = stripeService;
    }

    /**
     * Get the current subscription for a user
     *
     * @param email User email
     * @return SubscriptionResponse containing subscription details
     * @throws SubscriptionException if user not found
     */
    public SubscriptionResponse getCurrentSubscription(String email) {
        Account account = findAccountByEmail(email);

        SubscriptionResponse response = new SubscriptionResponse();
        response.setEmail(email);
        response.setRemainingWeeklySends(account.getRemainingWeeklySends());
        
        if (account.getSubscriptionPlan() != null) {
            response.setPlan(account.getSubscriptionPlan());
            response.setActive(true);
        } else {
            response.setActive(false);
        }
        
        return response;
    }

    /**
     * Create a payment session for subscribing to a plan
     *
     * @param email  User email
     * @param planId Plan ID
     * @return Session information
     * @throws SubscriptionException if plan not found or session creation fails
     */
    public PaymentSessionResponse createCheckoutSession(String email, Long planId) {
        Account account = findAccountByEmail(email);
        SubscriptionPlan plan = planService.getPlanById(planId);

        SessionDto sessionDto = new SessionDto();
        sessionDto.setUserId(String.valueOf(account.getId()));

        Map<String, String> data = new HashMap<>();
        data.put("planId", planId.toString());
        data.put("email", account.getEmail());
        data.put("fullName", account.getNickname());
        sessionDto.setData(data);

        SessionDto result = stripeService.createSubscriptionSession(sessionDto);

        if (result.getMessage() != null) {
            throw new SubscriptionException("Failed to create payment session: " + result.getMessage());
        }

        PaymentSessionResponse response = new PaymentSessionResponse();
        response.setSessionUrl(result.getSessionUrl());
        response.setSessionId(result.getSessionId());
        response.setPlanName(plan.getName());
        response.setPlanPrice(plan.getPrice());
        
        return response;
    }

    /**
     * Create a test subscription without payment processing
     *
     * @param email  User email
     * @param planId Plan ID
     * @return SubscriptionResponse with the new subscription details
     * @throws SubscriptionException if plan not found
     */
    @Transactional
    public SubscriptionResponse createTestSubscription(String email, Long planId) {
        Account account = findAccountByEmail(email);
        SubscriptionPlan plan = planService.getPlanById(planId);

        account.setSubscriptionPlan(plan);
        account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
        account.setLastResetDate(LocalDateTime.now());
        accountRepository.save(account);

        SubscriptionResponse response = new SubscriptionResponse();
        response.setEmail(email);
        response.setPlan(plan);
        response.setActive(true);
        response.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
        
        return response;
    }

    /**
     * Helper method to find account by email
     *
     * @param email User email
     * @return Account
     * @throws SubscriptionException if account not found
     */
    private Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriptionException("User not found: " + email));
    }
}