package com.valedosol.kaju.feature.subscriptionPlan.controller;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.subscriptionPlan.model.SubscriptionPlan;
import com.valedosol.kaju.feature.subscriptionPlan.service.SubscriptionPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Assinaturas", description = "Endpoints para gerenciamento de planos de assinatura")
@SecurityRequirement(name = "Bearer Authentication")
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final AccountRepository accountRepository;

    public SubscriptionController(SubscriptionPlanService subscriptionPlanService,
            AccountRepository accountRepository) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.accountRepository = accountRepository;
    }

    @Operation(summary = "Listar todos os planos", description = "Retorna todos os planos de assinatura disponíveis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de planos retornada com sucesso", content = @Content(schema = @Schema(implementation = SubscriptionPlan.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionPlanService.getAllPlans();
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @Operation(summary = "Assinar um plano", description = "Redireciona o usuário para o checkout de pagamento para assinar um plano")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informações para redirecionamento de pagamento"),
            @ApiResponse(responseCode = "404", description = "Usuário ou plano não encontrado")
    })
    @PostMapping("/subscribe/{planId}")
    public ResponseEntity<?> subscribe(@PathVariable Long planId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        try {
            SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId);

            // Em vez de atualizar diretamente a assinatura, retornamos informações para
            // redirecionar o usuário para o checkout do Stripe
            return new ResponseEntity<>(
                    Map.of(
                            "redirectToPayment", true,
                            "planId", planId,
                            "planName", plan.getName(),
                            "planPrice", plan.getPrice()),
                    HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Plan not found", HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Assinar um plano (teste)", description = "Assina um plano sem processamento de pagamento (apenas para testes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assinatura realizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou plano não encontrado")
    })
    @PostMapping("/subscribe/test/{planId}")
    public ResponseEntity<?> subscribeTest(@PathVariable Long planId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        try {
            SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId);

            Account account = accountOpt.get();

            // Here you would implement payment processing
            // For now, we'll just update the subscription

            account.setSubscriptionPlan(plan);
            account.setRemainingWeeklySends(plan.getWeeklyAllowedSends());
            account.setLastResetDate(LocalDateTime.now());

            accountRepository.save(account);

            return new ResponseEntity<>("Subscribed to " + plan.getName(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Plan not found", HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Obter meu plano", description = "Retorna o plano atual do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
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
                        "remainingSends", account.getRemainingWeeklySends()),
                HttpStatus.OK);
    }
}