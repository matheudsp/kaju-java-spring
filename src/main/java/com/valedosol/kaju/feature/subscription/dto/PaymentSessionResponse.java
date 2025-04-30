package com.valedosol.kaju.feature.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response with payment session details")
public class PaymentSessionResponse {
    @Schema(description = "URL da sessão de pagamento", example = "https://checkout.stripe.com/c/pay/...")
    private String sessionUrl;
    
    @Schema(description = "ID da sessão de pagamento", example = "cs_test_a1b2c3...")
    private String sessionId;
    
    @Schema(description = "Nome do plano", example = "Ouro")
    private String planName;
    
    @Schema(description = "Preço do plano", example = "50.00")
    private Double planPrice;
}