package com.valedosol.kaju.feature.auth.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Requisição de login de usuário")
public class LoginRequest {
    @Schema(description = "Email do usuário", example = "joao.silva@exemplo.com", required = true)
    private String email;
    
    @Schema(description = "Senha do usuário", example = "senha123", required = true)
    private String password;
}