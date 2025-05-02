package com.valedosol.kaju.feature.whatsapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.config.UserContext;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.whatsapp.dto.CreateInstanceRequest;
import com.valedosol.kaju.feature.whatsapp.dto.WhatsAppInstanceDTO;
import com.valedosol.kaju.feature.whatsapp.service.WhatsAppInstanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp/instances")
@Tag(name = "WhatsApp Instances", description = "API para gerenciamento de instâncias do WhatsApp")
@Slf4j
public class WhatsAppInstanceController {

    private final WhatsAppInstanceService whatsAppInstanceService;
    private final AccountRepository accountRepository;
    private final UserContext userContext;

    public WhatsAppInstanceController(
            WhatsAppInstanceService whatsAppInstanceService,
            AccountRepository accountRepository,
            UserContext userContext) {
        this.whatsAppInstanceService = whatsAppInstanceService;
        this.accountRepository = accountRepository;
        this.userContext = userContext;
    }

    @PostMapping
    @Operation(summary = "Criar instância", description = "Cria uma nova instância do WhatsApp para o usuário atual")
    public ResponseEntity<WhatsAppInstanceDTO> createInstance(
            @Valid @RequestBody CreateInstanceRequest request) {
        log.info("Creating WhatsApp instance with name: {}", request.getInstanceName());
        WhatsAppInstanceDTO instance = whatsAppInstanceService.createInstance(request);
        return new ResponseEntity<>(instance, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar instâncias", description = "Lista todas as instâncias do usuário atual")
    public ResponseEntity<List<WhatsAppInstanceDTO>> getAllInstances() {
        Account account = getCurrentAccount();
        List<WhatsAppInstanceDTO> instances = whatsAppInstanceService.getUserInstances(account);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter instância", description = "Obtém os detalhes de uma instância específica")
    public ResponseEntity<WhatsAppInstanceDTO> getInstance(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        WhatsAppInstanceDTO instance = whatsAppInstanceService.getInstance(id);
        return ResponseEntity.ok(instance);
    }

    @PostMapping("/{id}/connect")
    @Operation(summary = "Conectar instância", description = "Conecta a uma instância do WhatsApp")
    public ResponseEntity<WhatsAppInstanceDTO> connectInstance(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        Account account = getCurrentAccount();
        WhatsAppInstanceDTO instance = whatsAppInstanceService.connectInstance(id, account);
        return ResponseEntity.ok(instance);
    }

    @PostMapping("/{id}/restart")
    @Operation(summary = "Reiniciar instância", description = "Reinicia uma instância do WhatsApp")
    public ResponseEntity<WhatsAppInstanceDTO> restartInstance(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        Account account = getCurrentAccount();
        WhatsAppInstanceDTO instance = whatsAppInstanceService.restartInstance(id, account);
        return ResponseEntity.ok(instance);
    }

    @PostMapping("/{id}/logout")
    @Operation(summary = "Desconectar instância", description = "Desconecta uma instância do WhatsApp")
    public ResponseEntity<Map<String, String>> logoutInstance(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        Account account = getCurrentAccount();
        whatsAppInstanceService.logoutInstance(id, account);
        return ResponseEntity.ok(Map.of("message", "WhatsApp instance logged out successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir instância", description = "Exclui uma instância do WhatsApp")
    public ResponseEntity<Map<String, String>> deleteInstance(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        Account account = getCurrentAccount();
        whatsAppInstanceService.deleteInstance(id, account);
        return ResponseEntity.ok(Map.of("message", "WhatsApp instance deleted successfully"));
    }

    @GetMapping("/{id}/state")
    @Operation(summary = "Estado da conexão", description = "Obtém o estado atual da conexão de uma instância")
    public ResponseEntity<WhatsAppInstanceDTO> getConnectionState(
            @Parameter(description = "ID da instância") @PathVariable Long id) {
        Account account = getCurrentAccount();
        WhatsAppInstanceDTO state = whatsAppInstanceService.getConnectionState(id, account);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/update-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar status de todas instâncias", description = "Atualiza o status de todas as instâncias (apenas Admin)")
    public ResponseEntity<Map<String, String>> updateAllInstancesStatus() {
        whatsAppInstanceService.updateAllInstancesStatus();
        return ResponseEntity.ok(Map.of("message", "All WhatsApp instances status updated successfully"));
    }

    private Account getCurrentAccount() {
        String email = userContext.getCurrentUserEmail();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}