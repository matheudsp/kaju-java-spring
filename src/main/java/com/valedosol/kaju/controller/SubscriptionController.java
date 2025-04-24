package com.valedosol.kaju.controller;

import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.AccountRepository;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final AccountRepository accountRepository;

    public SubscriptionController(SubscriptionPlanRepository subscriptionPlanRepository,
                                  AccountRepository accountRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @PostMapping("/subscribe/{planId}")
    public ResponseEntity<?> subscribe(@PathVariable Long planId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(planId);
        if (!planOpt.isPresent()) {
            return new ResponseEntity<>("Plan not found", HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();
        SubscriptionPlan plan = planOpt.get();

        // Here you would implement payment processing
        // For now, we'll just update the subscription

        account.setSubscriptionPlan(plan);
        account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
        account.setLastResetDate(LocalDateTime.now());

        accountRepository.save(account);

        return new ResponseEntity<>("Subscribed to " + plan.getName(), HttpStatus.OK);
    }

    @GetMapping("/my-plan")
    public ResponseEntity<?> getMyPlan() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();
        if (account.getSubscriptionPlan() == null) {
            return new ResponseEntity<>("No active subscription", HttpStatus.OK);
        }

        return new ResponseEntity<>(
                Map.of(
                        "plan", account.getSubscriptionPlan(),
                        "remainingSends", account.getRemainingWeeklySends()
                ),
                HttpStatus.OK
        );
    }
}