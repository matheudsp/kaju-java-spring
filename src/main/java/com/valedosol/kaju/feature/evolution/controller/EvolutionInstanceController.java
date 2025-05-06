package com.valedosol.kaju.feature.evolution.controller;

import com.valedosol.kaju.feature.evolution.dto.EvolutionInstanceDTO;
import com.valedosol.kaju.feature.evolution.model.EvolutionInstance;
import com.valedosol.kaju.feature.evolution.service.EvolutionApiService;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/instances")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Instances", description = "Endpoints to manage WhatsApp instances")
public class EvolutionInstanceController {

    private final EvolutionApiService evolutionApiService;

    @Operation(summary = "Get all user instances", description = "Retrieves all WhatsApp instances owned by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of instances retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<List<EvolutionInstanceDTO>> getUserInstances() {
        List<EvolutionInstance> instances = evolutionApiService.getUserInstances();
        List<EvolutionInstanceDTO> dtos = instances.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get instance details", description = "Retrieves details of a specific WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instance details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{instanceName}")
    public ResponseEntity<EvolutionInstanceDTO> getInstanceDetails(@PathVariable String instanceName) {
        EvolutionInstance instance = evolutionApiService.getInstance(instanceName);
        return ResponseEntity.ok(convertToDTO(instance));
    }

    @Operation(summary = "Check instance connection status", description = "Checks the connection status of a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{instanceName}/status")
    public ResponseEntity<?> checkInstanceStatus(@PathVariable String instanceName) {
        return ResponseEntity.ok(evolutionApiService.getConnectionState(instanceName));
    }

    @Operation(summary = "Connect to an instance", description = "Connects to a WhatsApp instance, generating QR code if needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection initiated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/{instanceName}/connect")
    public ResponseEntity<?> connectToInstance(@PathVariable String instanceName) {
        return ResponseEntity.ok(evolutionApiService.connectInstance(instanceName));
    }

    @Operation(summary = "Restart an instance", description = "Restarts a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instance restarted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/{instanceName}/restart")
    public ResponseEntity<?> restartInstance(@PathVariable String instanceName) {
        return ResponseEntity.ok(evolutionApiService.restartInstance(instanceName));
    }

    @Operation(summary = "Logout from an instance", description = "Logs out from a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/{instanceName}/logout")
    public ResponseEntity<?> logoutFromInstance(@PathVariable String instanceName) {
        evolutionApiService.logoutInstance(instanceName);
        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }

    @Operation(summary = "Delete an instance", description = "Deletes a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instance deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{instanceName}")
    public ResponseEntity<?> deleteInstance(@PathVariable String instanceName) {
        evolutionApiService.deleteInstance(instanceName);
        return ResponseEntity.ok().body(Map.of("message", "Instance deleted successfully"));
    }

    private EvolutionInstanceDTO convertToDTO(EvolutionInstance instance) {
        return EvolutionInstanceDTO.builder()
                .id(instance.getId())
                .instanceName(instance.getInstanceName())
                .phoneNumber(instance.getPhoneNumber())
                .status(instance.getStatus().name())
                .createdAt(instance.getCreatedAt())
                .lastConnected(instance.getLastConnected())
                .build();
    }
}