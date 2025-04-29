package com.valedosol.kaju.feature.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Requisição de registro de usuário")
public class SignupRequest {
    @Schema(description = "Nome do usuário", example = "João Silva", required = true)
    private String name;
    
    @Schema(description = "Email do usuário", example = "joao.silva@exemplo.com", required = true)
    private String email;
    
    @Schema(description = "Senha do usuário", example = "senha123", required = true)
    private String password;
}