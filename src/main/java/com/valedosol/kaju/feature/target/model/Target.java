package com.valedosol.kaju.feature.target.model;

import com.valedosol.kaju.feature.auth.model.Account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Alvo de envio de promoções")
public class Target {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID do alvo", example = "1")
    private Long id;

    @Schema(description = "Nome do alvo", example = "Grupo do Kaju")
    private String name;

    @Schema(description = "Identificador do alvo", example = "120363397285478228@g.us")
    private String identifier;

    @Schema(description = "Tipo do alvo", example = "channel", allowableValues = { "channel", "group", "newsletter" })
    private String type; // "channel" ou "group" ou "newsletter"

    @Schema(description = "Descrição do alvo", example = "Grupo oficial do Kaju para discussões e promoções")
    private String description;

    @ManyToOne
    @JoinColumn(name = "account_id")
    @Schema(description = "Proprietário do alvo (null para alvos globais)")
    private Account owner; // null indicates a global target available to all users

    // Constructor for global targets
    public Target(String name, String identifier, String type, String description) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
        this.description = description;
        // owner intentionally left null
    }

    // Constructor for user-specific targets
    public Target(String name, String identifier, String type, String description, Account owner) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
        this.description = description;
        this.owner = owner;
    }

    // Helper method to check if target is global
    @Schema(description = "Verifica se o alvo é global (disponível para todos os usuários)")
    public boolean isGlobal() {
        return owner == null;
    }
}