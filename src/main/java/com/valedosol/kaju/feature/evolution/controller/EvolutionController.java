package com.valedosol.kaju.feature.evolution.controller;

import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.ConnectionStateResponse;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.CreateInstanceRequest;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.InstanceResponse;
import com.valedosol.kaju.feature.evolution.model.EvolutionInstance;
import com.valedosol.kaju.feature.evolution.service.EvolutionApiService;

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

@RestController
@RequestMapping("/api/v1/evolution")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Integration", description = "Endpoints to manage WhatsApp instances via Evolution API")
public class EvolutionController {

    private final EvolutionApiService evolutionApiService;

    @Operation(summary = "Create a new WhatsApp instance", description = "Creates a new WhatsApp instance through Evolution API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instance created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/instances")
    public ResponseEntity<InstanceResponse> createInstance(@RequestBody CreateInstanceRequest request) {
        return ResponseEntity.ok(evolutionApiService.createInstance(request));
    }

    @Operation(summary = "Get all user instances", description = "Retrieves all WhatsApp instances owned by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of instances retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/instances")
    public ResponseEntity<List<EvolutionInstance>> getUserInstances() {
        return ResponseEntity.ok(evolutionApiService.getUserInstances());
    }

    @Operation(summary = "Get instance connection state", description = "Retrieves the connection state of a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection state retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/instances/{instanceName}/status")
    public ResponseEntity<ConnectionStateResponse> getConnectionState(@PathVariable String instanceName) {
        return ResponseEntity.ok(evolutionApiService.getConnectionState(instanceName));
    }

    @Operation(summary = "Connect to WhatsApp instance", description = "Connects to a WhatsApp instance, generating QR code if needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Instance not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/instances/{instanceName}/connect")
    public ResponseEntity<InstanceResponse> connectInstance(@PathVariable String instanceName) {
        return ResponseEntity.ok(evolutionApiService.connectInstance(instanceName));
    }

    @Operation(summary = "Restart WhatsApp instance", description = "Restarts a WhatsApp instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Instance restarted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(