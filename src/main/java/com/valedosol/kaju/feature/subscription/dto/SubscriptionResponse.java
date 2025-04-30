package com.valedosol.kaju.feature.subscription.dto;

import com.valedosol.kaju.feature.subscription.model.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Informações sobre a assinatura atual do usuário")
public class SubscriptionResponse {
    @Schema(description = "Email do usuário", example = "usuario@exemplo.com")
    private String email;
    
    @Schema(description = "Plano de assinatura atual")
    private SubscriptionPlan plan;
    
    @Schema(description = "Indica se a assinatura está ativa", example = "true")
    private boolean active;
    
    @Schema(description = "Envios restantes na semana atual", example = "2")
    private Integer remainingWeeklySends;
}