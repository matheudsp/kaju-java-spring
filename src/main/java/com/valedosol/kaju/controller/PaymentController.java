// package com.valedosol.kaju.controller;

// import com.stripe.exception.StripeException;
// import com.stripe.model.PaymentIntent;
// import com.valedosol.kaju.model.Account;
// import com.valedosol.kaju.model.StripeSubscription;
// import com.valedosol.kaju.model.SubscriptionPlan;
// import com.valedosol.kaju.repository.AccountRepository;
// import com.valedosol.kaju.repository.StripeSubscriptionRepository;
// import com.valedosol.kaju.repository.SubscriptionPlanRepository;
// import com.valedosol.kaju.service.StripeService;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.Optional;

// @RestController
// @RequestMapping("/api/payments")
// public class PaymentController {

//     private final StripeService stripeService;
//     private final AccountRepository accountRepository;
//     private final SubscriptionPlanRepository subscriptionPlanRepository;
//     private final StripeSubscriptionRepository stripeSubscriptionRepository;
    
//     public PaymentController(StripeService stripeService,
//             AccountRepository accountRepository,
//             SubscriptionPlanRepository subscriptionPlanRepository,
//             StripeSubscriptionRepository stripeSubscriptionRepository) {
//         this.stripeService = stripeService;
//         this.accountRepository = accountRepository;
//         this.subscriptionPlanRepository = subscriptionPlanRepository;
//         this.stripeSubscriptionRepository = stripeSubscriptionRepository;
//     }

//     @PostMapping("/create-payment-intent")
//     public ResponseEntity<?> createPaymentIntent(@RequestParam double amount) {
//         try {
//             PaymentIntent paymentIntent = stripeService.createPaymentIntent(amount, "brl");

//             Map<String, String> response = new HashMap<>();
//             response.put("clientSecret", paymentIntent.getClientSecret());

//             return new ResponseEntity<>(response, HttpStatus.OK);
//         } catch (StripeException e) {
//             return new ResponseEntity<>("Error creating payment intent: " + e.getMessage(),
//                     HttpStatus.INTERNAL_SERVER_ERROR);
//         }
//     }

//     @PostMapping("/create-checkout-session/{planId}")
//     public ResponseEntity<?> createCheckoutSession(@PathVariable Long planId) {
//         try {
//             // Obter usuário autenticado
//             Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//             String email = auth.getName();

//             Optional<Account> accountOpt = accountRepository.findByEmail(email);
//             if (!accountOpt.isPresent()) {
//                 return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
//             }

//             Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findById(planId);
//             if (!planOpt.isPresent()) {
//                 return new ResponseEntity<>("Plan not found", HttpStatus.NOT_FOUND);
//             }

//             Account account = accountOpt.get();
//             SubscriptionPlan plan = planOpt.get();

//             // Gerar URLs de sucesso e cancelamento
//             String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
//             String successUrl = baseUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}";
//             String cancelUrl = baseUrl + "/payment-cancel";

//             // Criar sessão de checkout
//             Map<String, Object> checkoutData = stripeService.createCheckoutSession(account, plan, successUrl,
//                     cancelUrl);

//             return new ResponseEntity<>(checkoutData, HttpStatus.OK);
//         } catch (StripeException e) {
//             return new ResponseEntity<>("Error creating checkout session: " + e.getMessage(),
//                     HttpStatus.INTERNAL_SERVER_ERROR);
//         }
//     }

//     @PostMapping("/webhook")
//     public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
//             @RequestHeader("Stripe-Signature") String sigHeader) {
//         try {
//             stripeService.processWebhookEvent(payload, sigHeader);
//             return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
//         } catch (StripeException e) {
//             return new ResponseEntity<>("Webhook error: " + e.getMessage(),
//                     HttpStatus.BAD_REQUEST);
//         }
//     }

//     @PostMapping("/cancel-subscription")
//     public ResponseEntity<?> cancelSubscription() {
//         try {
//             // Get authenticated user
//             Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//             String email = auth.getName();

//             Optional<Account> accountOpt = accountRepository.findByEmail(email);
//             if (!accountOpt.isPresent()) {
//                 return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
//             }

//             Account account = accountOpt.get();

//             // Find the active Stripe subscription
//             Optional<StripeSubscription> stripeSubscriptionOpt = stripeSubscriptionRepository
//                     .findByAccountId(account.getId());
//             if (!stripeSubscriptionOpt.isPresent() || stripeSubscriptionOpt.get().getStripeSubscriptionId() == null) {
//                 return new ResponseEntity<>("No active subscription found", HttpStatus.NOT_FOUND);
//             }

//             // Cancel subscription in Stripe
//             String stripeSubscriptionId = stripeSubscriptionOpt.get().getStripeSubscriptionId();
//             stripeService.cancelSubscription(stripeSubscriptionId);

//             return new ResponseEntity<>("Subscription canceled successfully", HttpStatus.OK);
//         } catch (Exception e) {
//             return new ResponseEntity<>("Error canceling subscription: " + e.getMessage(),
//                     HttpStatus.INTERNAL_SERVER_ERROR);
//         }
//     }
// }