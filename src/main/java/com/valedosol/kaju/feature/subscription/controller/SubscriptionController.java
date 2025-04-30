package com.valedosol.kaju.feature.subscription.controller;

import com.valedosol.kaju.config.UserContext;
import com.valedosol.kaju.feature.subscription.dto.PaymentSessionRequest;
import com.valedosol.kaju.feature.subscription.dto.SubscriptionResponse;
import com.valedosol.kaju.feature.subscription.model.SubscriptionPlan;
import com.valedosol.kaju.feature.subscription.service.SubscriptionPlanService;
import com.valedosol.kaju.feature.subscription.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller to handle all subscription-related operations
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Assinaturas", description = "Endpoints para gerenciamento de assinaturas e planos")
@SecurityRequirement(name = "Bearer Authentication")
public class SubscriptionController {

    private final SubscriptionPlanService planService;
    private final SubscriptionService subscriptionService;
    private final UserContext userContext;

    public SubscriptionController(
            SubscriptionPlanService planService,
            SubscriptionService subscriptionService,
            UserContext userContext) {
        this.planService = planService;
        this.subscriptionService = subscriptionService;
        this.userContext = userContext;
    }

    @Operation(summary = "Listar todos os planos", description = "Retorna todos os planos de assinatura disponíveis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de planos retornada com sucesso", 
                    content = @Content(schema = @Schema(implementation = SubscriptionPlan.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @Operation(summary = "Obter plano atual", description = "Retorna o plano atual do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano atual retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/my-plan")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription() {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userContext.getCurrentUserEmail()));
    }

    @Operation(summary = "Criar sessão de pagamento", description = "Cria uma sessão de pagamento para assinar um plano")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessão de pagamento criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(@RequestBody PaymentSessionRequest request) {
        var sessionResult = subscriptionService.createCheckoutSession(
                userContext.getCurrentUserEmail(), 
                request.getPlanId());
        return ResponseEntity.ok(sessionResult);
    }

    @Operation(summary = "Assinar plano (teste)", description = "Assina um plano sem processamento de pagamento (apenas para testes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assinatura realizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou plano não encontrado")
    })
    @PostMapping("/test/{planId}")
    public ResponseEntity<SubscriptionResponse> subscribeForTesting(@PathVariable Long planId) {
        SubscriptionResponse subscription = subscriptionService.createTestSubscription(
                userContext.getCurrentUserEmail(), planId);
        return ResponseEntity.ok(subscription);
    }
}