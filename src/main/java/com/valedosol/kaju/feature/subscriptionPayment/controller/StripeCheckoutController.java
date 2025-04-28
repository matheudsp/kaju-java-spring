package com.valedosol.kaju.feature.subscriptionPayment.controller;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.subscriptionPayment.dto.SessionDto;
import com.valedosol.kaju.feature.subscriptionPayment.service.StripeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe/checkout")
public class StripeCheckoutController {

    private final StripeService stripeService;
    private final AccountRepository accountRepository;

    @Autowired
    public StripeCheckoutController(StripeService stripeService, AccountRepository accountRepository) {
        this.stripeService = stripeService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/create-subscription")
    public ResponseEntity<?> createSubscriptionSession(@RequestBody Map<String, String> payload,
            Authentication authentication) {
        try {
            // Get authenticated user
            Object principal = authentication.getPrincipal();
            String email;

            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            Optional<Account> accountOpt = accountRepository.findByEmail(email);

            if (accountOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "User account not found"));
            }

            Account account = accountOpt.get();

            // Validate planId
            String planId = payload.get("planId");
            if (planId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "ID do plano é necessário."));
            }

            // Create session dto with required data
            SessionDto sessionDto = new SessionDto();
            sessionDto.setUserId(String.valueOf(account.getId()));

            Map<String, String> data = new HashMap<>();
            data.put("planId", planId);
            data.put("email", account.getEmail());
            data.put("fullName", account.getNickname()); 
            sessionDto.setData(data);

            // Create subscription session
            SessionDto result = stripeService.createSubscriptionSession(sessionDto);

            if (result.getMessage() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", result.getMessage()));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create subscription: " + e.getMessage()));
        }
    }
}