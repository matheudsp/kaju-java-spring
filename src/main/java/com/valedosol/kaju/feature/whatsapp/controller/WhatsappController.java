package com.valedosol.kaju.feature.whatsapp.controller;

import com.valedosol.kaju.feature.whatsapp.dto.WhatsappInstanceListResponse;
import com.valedosol.kaju.feature.whatsapp.dto.WhatsappInstanceResponse;
import com.valedosol.kaju.feature.whatsapp.model.WhatsappInstance;
import com.valedosol.kaju.feature.whatsapp.service.WhatsappService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "Whatsapp", description = "Operations for managing Whatsapp integrations")
public class WhatsappController {

  private final WhatsappService WhatsappService;

  public WhatsappController(WhatsappService WhatsappService) {
    this.WhatsappService = WhatsappService;
  }

  @Operation(summary = "Create a new Whatsapp instance", description = "Creates a new Whatsapp instance linked to the user account", security = @SecurityRequirement(name = "JWT"))
  @PostMapping("/instances")
  public ResponseEntity<WhatsappInstanceResponse> createInstance(@RequestBody Map<String, String> request) {
    String instanceName = request.get("instanceName");
    String accountEmail = getCurrentUserEmail();

    WhatsappInstanceResponse response = WhatsappService.createInstance(instanceName, accountEmail);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Connect to a Whatsapp instance", description = "Connects to an existing Whatsapp instance and returns QR code if needed", security = @SecurityRequirement(name = "JWT"))
  @GetMapping("/instances/{instanceName}/connect")
  public ResponseEntity<WhatsappInstanceResponse> connectInstance(@PathVariable String instanceName) {
    String accountEmail = getCurrentUserEmail();

    WhatsappInstanceResponse response = WhatsappService.connectInstance(instanceName, accountEmail);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get connection state", description = "Gets the connection state of a Whatsapp instance", security = @SecurityRequirement(name = "JWT"))
  @GetMapping("/instances/{instanceName}/state")
  public ResponseEntity<Map<String, String>> getConnectionState(@PathVariable String instanceName) {
    String accountEmail = getCurrentUserEmail();

    String state = WhatsappService.getConnectionState(instanceName, accountEmail);
    return ResponseEntity.ok(Map.of("state", state));
  }

  @Operation(summary = "Logout from a Whatsapp instance", description = "Logs out from a Whatsapp instance", security = @SecurityRequirement(name = "JWT"))
  @PostMapping("/instances/{instanceName}/logout")
  public ResponseEntity<Map<String, String>> logoutInstance(@PathVariable String instanceName) {
    String accountEmail = getCurrentUserEmail();

    WhatsappService.logoutInstance(instanceName, accountEmail);
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  @Operation(summary = "Delete a Whatsapp instance", description = "Deletes a Whatsapp instance permanently", security = @SecurityRequirement(name = "JWT"))
  @DeleteMapping("/instances/{instanceName}")
  public ResponseEntity<Map<String, String>> deleteInstance(@PathVariable String instanceName) {
    String accountEmail = getCurrentUserEmail();

    WhatsappService.deleteInstance(instanceName, accountEmail);
    return ResponseEntity.ok(Map.of("message", "Instance deleted successfully"));
  }

  @Operation(summary = "List user's Whatsapp instances", description = "Lists all Whatsapp instances for the authenticated user", security = @SecurityRequirement(name = "JWT"))
  @GetMapping("/instances")
  public ResponseEntity<List<WhatsappInstance>> getUserInstances() {
    String accountEmail = getCurrentUserEmail();

    List<WhatsappInstance> instances = WhatsappService.getUserInstances(accountEmail);
    return ResponseEntity.ok(instances);
  }

  @Operation(summary = "Restart Whatsapp instance", description = "Restarts a Whatsapp instance", security = @SecurityRequirement(name = "JWT"))
  @PostMapping("/instances/{instanceName}/restart")
  public ResponseEntity<WhatsappInstanceResponse> restartInstance(@PathVariable String instanceName) {
    String accountEmail = getCurrentUserEmail();

    WhatsappInstanceResponse response = WhatsappService.restartInstance(instanceName, accountEmail);
    return ResponseEntity.ok(response);
  }

  // @Operation(summary = "Fetch all instances (Admin)", description = "Fetches all Whatsapp instances from Evolution API (Admin only)", security = @SecurityRequirement(name = "JWT"))
  // @GetMapping("/admin/instances")
  // public ResponseEntity<WhatsappInstanceListResponse> fetchAllInstances() {
  //   // TODO: Add admin role check
  //   WhatsappInstanceListResponse response = WhatsappService.fetchAllInstances();
  //   return ResponseEntity.ok(response);
  // }

  private String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

}