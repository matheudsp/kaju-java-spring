package com.valedosol.kaju.feature.subscription.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "subscription_plan")
@Schema(description = "Plano de assinatura")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID do plano", example = "1")
    private Long id;
    
    @NotBlank(message = "Nome do plano é obrigatório")
    @Schema(description = "Nome do plano", example = "Ouro")
    private String name;

    @NotNull(message = "Número de envios permitidos é obrigatório")
    @Positive(message = "Número de envios deve ser positivo")
    @Schema(description = "Número de envios permitidos por semana", example = "3")
    private Integer weeklyAllowedSends;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    @Schema(description = "Preço do plano em reais", example = "50.00")
    private Double price;

    @Schema(description = "ID do produto no Stripe", example = "prod_abc123")
    private String stripeProductId;

    @Schema(description = "ID do preço no Stripe", example = "price_xyz789")
    private String stripePriceId;

    @Schema(description = "Intervalo de cobrança", example = "month", allowableValues = { "month", "year" })
    private String billingInterval = "month"; // "month" or "year"

    // Constructor with required fields
    public SubscriptionPlan(String name, Integer weeklyAllowedSends, Double price) {
        this.name = name;
        this.weeklyAllowedSends = weeklyAllowedSends;
        this.price = price;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionPlan that = (SubscriptionPlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}