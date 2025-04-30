package com.valedosol.kaju.feature.subscription.service;

import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.subscription.dto.PaymentSessionResponse;
import com.valedosol.kaju.feature.subscription.dto.SessionDto;
import com.valedosol.kaju.feature.subscription.dto.SubscriptionResponse;
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
     */
    public SubscriptionResponse getCurrentSubscription(String email) {
        Account account = findAccountByEmail(email);

        return buildSubscriptionResponse(account);
    }

    private SubscriptionResponse buildSubscriptionResponse(Account account) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setEmail(account.getEmail());
        response.setRemainingWeeklySends(account.getRemainingWeeklySends());
        
        SubscriptionPlan plan = account.getSubscriptionPlan();
        if (plan != null) {
            response.setPlan(plan);
            response.setActive(true);
        } else {
            response.setActive(false);
        }
        
        return response;
    }

    /**
     * Create a payment session for subscribing to a plan
     */
    public PaymentSessionResponse createCheckoutSession(String email, Long planId) {
        Account account = findAccountByEmail(email);
        SubscriptionPlan plan = planService.getPlanById(planId);

        // Create a SessionDto with the necessary data
        SessionDto sessionDto = new SessionDto();
        sessionDto.setUserId(String.valueOf(account.getId()));
        
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("planId", planId.toString());
        sessionData.put("email", account.getEmail());
        sessionData.put("fullName", account.getNickname());
        sessionDto.setData(sessionData);

        // Call the service and get the result
        SessionDto sessionResult = stripeService.createSubscriptionSession(sessionDto);

        return PaymentSessionResponse.builder()
                .sessionUrl(sessionResult.getSessionUrl())
                .sessionId(sessionResult.getSessionId())
                .planName(plan.getName())
                .planPrice(plan.getPrice())
                .build();
    }

    /**
     * Create a test subscription without payment processing
     */
    @Transactional
    public SubscriptionResponse createTestSubscription(String email, Long planId) {
        Account account = findAccountByEmail(email);
        SubscriptionPlan plan = planService.getPlanById(planId);

        account.setSubscriptionPlan(plan);
        account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
        account.setLastResetDate(LocalDateTime.now());
        accountRepository.save(account);

        return buildSubscriptionResponse(account);
    }

    /**
     * Helper method to find account by email
     */
    private Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}