package com.valedosol.kaju.feature.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to create a payment session")
public class PaymentSessionRequest {
    @Schema(description = "ID do plano desejado", example = "1", required = true)
    private Long planId;
}