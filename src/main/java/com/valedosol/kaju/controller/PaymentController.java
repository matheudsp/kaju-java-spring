package com.valedosol.kaju.controller;

import com.stripe.exception.StripeException;
import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.AccountRepository;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;
import com.valedosol.kaju.service.StripeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final StripeService stripeService;
    private final AccountRepository accountRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public PaymentController(StripeService stripeService,
                            AccountRepository accountRepository,
                            SubscriptionPlanRepository subscriptionPlanRepository) {
        this.stripeService = stripeService;
        this.accountRepository = accountRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @GetMapping("/config")
    public ResponseEntity<String> getStripePublicKey() {
        return new ResponseEntity<>(stripeService.getPublicKey(), HttpStatus.OK);
    }

    @PostMapping("/create-checkout-session/{planId}")
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long planId) {
        try {
            // Obter usuário autenticado
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

            // Gerar URLs de sucesso e cancelamento
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String successUrl = baseUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = baseUrl + "/payment-cancel";

            // Criar sessão de checkout
            Map<String, Object> checkoutData = stripeService.createCheckoutSession(account, plan, successUrl, cancelUrl);
            
            return new ResponseEntity<>(checkoutData, HttpStatus.OK);
        } catch (StripeException e) {
            return new ResponseEntity<>("Error creating checkout session: " + e.getMessage(), 
                                       HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                     @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.processWebhookEvent(payload, sigHeader);
            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (StripeException e) {
            return new ResponseEntity<>("Webhook error: " + e.getMessage(), 
                                       HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<?> cancelSubscription() {
        try {
            // Obter usuário autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Optional<Account> accountOpt = accountRepository.findByEmail(email);
            if (!accountOpt.isPresent()) {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }

            // Aqui você precisaria obter o ID da assinatura do Stripe associada ao usuário
            // Para fins de exemplo, assumimos que você tenha um método para isso
            
            // Lógica para cancelar a assinatura no Stripe
            // stripeService.cancelSubscription(stripeSubscriptionId);
            
            return new ResponseEntity<>("Subscription canceled successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error canceling subscription: " + e.getMessage(), 
                                       HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}