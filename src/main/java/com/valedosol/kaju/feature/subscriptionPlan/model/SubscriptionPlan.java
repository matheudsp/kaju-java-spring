package com.valedosol.kaju.feature.subscriptionPlan.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Plano de assinatura")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID do plano", example = "1")
    private Long id;
    @Schema(description = "Nome do plano", example = "Ouro")
    private String name;

    @Schema(description = "Número de envios permitidos por semana", example = "3")
    private Integer weeklyAllowedSends;

    @Schema(description = "Preço do plano em reais", example = "50.00")
    private Double price;

    @Schema(description = "ID do produto no Stripe", example = "prod_abc123")
    private String stripeProductId;

    @Schema(description = "ID do preço no Stripe", example = "price_xyz789")
    private String stripePriceId;

    @Schema(description = "Intervalo de cobrança", example = "month", allowableValues = { "month", "year" })
    private String billingInterval = "month"; // "month" or "year"

    // Constructor
    public SubscriptionPlan(String name, Integer weeklyAllowedSends, Double price) {
        this.name = name;
        this.weeklyAllowedSends = weeklyAllowedSends;
        this.price = price;
    }
}