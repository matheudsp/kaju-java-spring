package com.valedosol.kaju.feature.evolution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.valedosol.kaju.common.exception.BusinessException;
import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.config.UserContext;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.auth.service.AccountService;
import com.valedosol.kaju.feature.target.model.Target;
import com.valedosol.kaju.feature.target.service.TargetService;
import com.valedosol.kaju.feature.evolution.client.WhatsappClient;
import com.valedosol.kaju.feature.evolution.config.WhatsappProperties;
import com.valedosol.kaju.feature.evolution.dto.MediaMessageRequest;
import com.valedosol.kaju.feature.evolution.dto.MessageResponse;
import com.valedosol.kaju.feature.evolution.model.WhatsappInstance;
import com.valedosol.kaju.feature.evolution.model.WhatsappInstance.InstanceStatus;
import com.valedosol.kaju.feature.evolution.repository.WhatsappInstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class WhatsappService {

  private final WhatsappClient whatsappClient;
  private final WhatsappInstanceRepository instanceRepository;
  private final AccountService accountService;
  private final AccountRepository accountRepository;
  private final TargetService targetService;
  private final UserContext userContext;
  private final WhatsappProperties whatsappProperties;

  public WhatsappService(
      WhatsappClient whatsappClient,
      WhatsappInstanceRepository instanceRepository,
      AccountService accountService,
      AccountRepository accountRepository,
      TargetService targetService,
      UserContext userContext,
      WhatsappProperties whatsappProperties) {
    this.whatsappClient = whatsappClient;
    this.instanceRepository = instanceRepository;
    this.accountService = accountService;
    this.accountRepository = accountRepository;
    this.targetService = targetService;
    this.userContext = userContext;
    this.whatsappProperties = whatsappProperties;
  }

  /**
   * Send a text message to a specific recipient, automatically selecting the
   * appropriate instance
   * 
   * @param to          The recipient phone number or group ID
   * @param messageText The message content
   * @return The message response
   */
  @Transactional
  public MessageResponse sendTextMessage(String to, String messageText) {
    WhatsappInstance instance = selectAppropriateInstance(to);
    instance.setLastUsedAt(Instant.now());
    instanceRepository.save(instance);

    return whatsappClient.sendText(instance.getToken(), instance.getInstanceId(), to, messageText);
  }

  /**
   * Send a media message to a specific recipient, automatically selecting the
   * appropriate instance
   * 
   * @param to           The recipient phone number or group ID
   * @param mediaRequest The media request object containing the media details
   * @return The message response
   */
  @Transactional
  public MessageResponse sendMediaMessage(String to, MediaMessageRequest mediaRequest) {
    WhatsappInstance instance = selectAppropriateInstance(to);
    instance.setLastUsedAt(Instant.now());
    instanceRepository.save(instance);

    return whatsappClient.sendMedia(instance.getToken(), instance.getInstanceId(), to, mediaRequest);
  }

  /**
   * Selects the appropriate WhatsApp instance based on the recipient
   * - For Kaju groups/channels: use global instance
   * - For regular recipients: use user's personal instance
   * 
   * @param to The recipient identifier
   * @return The selected WhatsApp instance
   */
  private WhatsappInstance selectAppropriateInstance(String to) {
    // Check if the recipient is a Kaju group or channel
    if (isKajuTarget(to)) {
      log.info("Using global instance for Kaju target: {}", to);
      return instanceRepository.findGlobalInstance()
          .orElseThrow(() -> new BusinessException("Global WhatsApp instance not configured"));
    } else {
      // Use current user's instance
      String currentUserEmail = userContext.getCurrentUserEmail();
      if (currentUserEmail == null) {
        throw new BusinessException("User not authenticated");
      }

      Account account = accountRepository.findByEmail(currentUserEmail)
          .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + currentUserEmail));

      return instanceRepository.findActiveInstanceByOwnerId(account.getId())
          .orElseThrow(() -> new BusinessException("User does not have an active WhatsApp instance"));
    }
  }

  /**
   * Check if a recipient is a predefined Kaju target (group or channel)
   * 
   * @param recipientId The recipient ID to check
   * @return true if it's a Kaju target, false otherwise
   */
  private boolean isKajuTarget(String recipientId) {
    try {
      Optional<Target> target = targetService.findByWaId(recipientId);
      return target.isPresent();
    } catch (Exception e) {
      log.error("Error checking if recipient is a Kaju target", e);
      return false;
    }
  }

  /**
   * Initialize a new WhatsApp instance for a user
   * 
   * @return The instance ID and status
   */
  @Transactional
  public JsonNode initializeUserInstance() {
    String currentUserEmail = userContext.getCurrentUserEmail();
    if (currentUserEmail == null) {
      throw new BusinessException("User not authenticated");
    }

    Account account = accountRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + currentUserEmail));

    // Generate unique instance ID for the user
    String instanceId = "user_" + account.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);

    // Create the instance in Evolution API
    JsonNode response = whatsappClient.createInstance(instanceId);

    if (response.has("success") && response.get("success").asBoolean()) {
      // Extract token
      String token = response.path("data").path("apikey").asText();

      // Save instance to database
      WhatsappInstance instance = WhatsappInstance.builder()
          .instanceId(instanceId)
          .token(token)
          .owner(account)
          .isGlobal(false)
          .status(InstanceStatus.CONNECTING)
          .createdAt(Instant.now())
          .build();

      instanceRepository.save(instance);

      // Return QR code for scanning
      return whatsappClient.getQrCode(token, instanceId);
    } else {
      throw new BusinessException("Failed to create WhatsApp instance: " +
          response.path("message").asText("Unknown error"));
    }
  }

  /**
   * Initialize the global WhatsApp instance (admin only)
   * 
   * @return The instance status
   */
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Transactional
  public JsonNode initializeGlobalInstance() {
    // Check if global instance already exists
    Optional<WhatsappInstance> existingGlobal = instanceRepository.findAll().stream()
        .filter(WhatsappInstance::isGlobal)
        .findFirst();

    if (existingGlobal.isPresent()) {
      throw new BusinessException("Global instance already exists");
    }

    // Generate instance ID for global instance
    String instanceId = "global_" + UUID.randomUUID().toString().substring(0, 8);

    // Create the instance in Evolution API
    JsonNode response = whatsappClient.createInstance(instanceId);

    if (response.has("success") && response.get("success").asBoolean()) {
      // Extract token
      String token = response.path("data").path("apikey").asText();

      // Save instance to database
      WhatsappInstance instance = WhatsappInstance.builder()
          .instanceId(instanceId)
          .token(token)
          .owner(null) // No owner for global instance
          .isGlobal(true)
          .status(InstanceStatus.CONNECTING)
          .createdAt(Instant.now())
          .build();

      instanceRepository.save(instance);

      // Return QR code for scanning
      return whatsappClient.getQrCode(token, instanceId);
    } else {
      throw new BusinessException("Failed to create global WhatsApp instance: " +
          response.path("message").asText("Unknown error"));
    }
  }

  /**
   * Check status of a user's WhatsApp instance
   * 
   * @return The instance status
   */
  public JsonNode getUserInstanceStatus() {
    String currentUserEmail = userContext.getCurrentUserEmail();
    if (currentUserEmail == null) {
      throw new BusinessException("User not authenticated");
    }

    Account account = accountRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + currentUserEmail));

    WhatsappInstance instance = instanceRepository.findByOwnerId(account.getId()).stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found for user"));

    JsonNode statusResponse = whatsappClient.getInstanceStatus(instance.getToken(), instance.getInstanceId());

    // Update instance status in database
    if (statusResponse.has("data") && statusResponse.get("data").has("status")) {
      String statusStr = statusResponse.get("data").get("status").asText();
      InstanceStatus status = parseStatus(statusStr);
      instance.setStatus(status);
      instanceRepository.save(instance);
    }

    return statusResponse;
  }

  /**
   * Check status of the global WhatsApp instance (admin only)
   * 
   * @return The instance status
   */
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public JsonNode getGlobalInstanceStatus() {
    WhatsappInstance globalInstance = instanceRepository.findGlobalInstance()
        .orElseThrow(() -> new ResourceNotFoundException("Global WhatsApp instance not found"));

    JsonNode statusResponse = whatsappClient.getInstanceStatus(globalInstance.getToken(),
        globalInstance.getInstanceId());

    // Update instance status in database
    if (statusResponse.has("data") && statusResponse.get("data").has("status")) {
      String statusStr = statusResponse.get("data").get("status").asText();
      InstanceStatus status = parseStatus(statusStr);
      globalInstance.setStatus(status);
      instanceRepository.save(globalInstance);
    }

    return statusResponse;
  }

  /**
   * Disconnect a user's WhatsApp instance
   * 
   * @return The disconnect response
   */
  @Transactional
  public JsonNode disconnectUserInstance() {
    String currentUserEmail = userContext.getCurrentUserEmail();
    if (currentUserEmail == null) {
      throw new BusinessException("User not authenticated");
    }

    Account account = accountRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + currentUserEmail));

    WhatsappInstance instance = instanceRepository.findByOwnerId(account.getId()).stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found for user"));

    JsonNode response = whatsappClient.disconnectInstance(instance.getToken(), instance.getInstanceId());

    // Update instance status
    instance.setStatus(InstanceStatus.DISCONNECTED);
    instanceRepository.save(instance);

    return response;
  }

  /**
   * Disconnect the global WhatsApp instance (admin only)
   * 
   * @return The disconnect response
   */
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Transactional
  public JsonNode disconnectGlobalInstance() {
    WhatsappInstance globalInstance = instanceRepository.findAll().stream()
        .filter(WhatsappInstance::isGlobal)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Global WhatsApp instance not found"));

    JsonNode response = whatsappClient.disconnectInstance(globalInstance.getToken(), globalInstance.getInstanceId());

    // Update instance status
    globalInstance.setStatus(InstanceStatus.DISCONNECTED);
    instanceRepository.save(globalInstance);

    return response;
  }

  /**
   * List all WhatsApp instances (admin only)
   * 
   * @return The list of instances
   */
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public List<WhatsappInstance> listAllInstances() {
    return instanceRepository.findAll();
  }

  /**
   * Scheduled task to update the status of all WhatsApp instances
   */
  @Scheduled(fixedRate = 300000) // Every 5 minutes
  @Transactional
  public void updateInstanceStatuses() {
    log.info("Running scheduled status update for WhatsApp instances");

    List<WhatsappInstance> instances = instanceRepository.findAll();
    for (WhatsappInstance instance : instances) {
      try {
        JsonNode statusResponse = whatsappClient.getInstanceStatus(
            instance.getToken(), instance.getInstanceId());

        if (statusResponse.has("data") && statusResponse.get("data").has("status")) {
          String statusStr = statusResponse.get("data").get("status").asText();
          InstanceStatus status = parseStatus(statusStr);
          instance.setStatus(status);
          instanceRepository.save(instance);
          log.info("Updated status for instance {}: {}", instance.getInstanceId(), status);
        }
      } catch (Exception e) {
        log.error("Error updating status for instance {}: {}", instance.getInstanceId(), e.getMessage());
        instance.setStatus(InstanceStatus.ERROR);
        instanceRepository.save(instance);
      }
    }
  }

  /**
   * Parse the status string from Evolution API to our enum
   * 
   * @param statusStr The status string from the API
   * @return The corresponding InstanceStatus enum value
   */
  private InstanceStatus parseStatus(String statusStr) {
    if (statusStr == null) {
      return InstanceStatus.ERROR;
    }

    switch (statusStr.toLowerCase()) {
      case "open":
      case "connected":
      case "active":
        return InstanceStatus.CONNECTED;
      case "connecting":
      case "loading":
      case "starting":
        return InstanceStatus.CONNECTING;
      case "disconnected":
      case "closed":
      case "logged_out":
        return InstanceStatus.DISCONNECTED;
      default:
        return InstanceStatus.ERROR;
    }
  }

  /**
   * Get the active instance for the current user
   * 
   * @return The user's active WhatsApp instance
   */
  public WhatsappInstance getCurrentUserActiveInstance() {
    String currentUserEmail = userContext.getCurrentUserEmail();
    if (currentUserEmail == null) {
      throw new BusinessException("User not authenticated");
    }

    Account account = accountRepository.findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + currentUserEmail));

    return instanceRepository.findActiveInstanceByOwnerId(account.getId())
        .orElseThrow(() -> new BusinessException("User does not have an active WhatsApp instance"));
  }

  /**
   * Delete a WhatsApp instance (admin only)
   * 
   * @param instanceId The ID of the instance to delete
   */
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @Transactional
  public void deleteInstance(Long instanceId) {
    WhatsappInstance instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found with ID: " + instanceId));

    // Try to disconnect from Evolution API first
    try {
      whatsappClient.disconnectInstance(instance.getToken(), instance.getInstanceId());
    } catch (Exception e) {
      log.warn("Failed to disconnect instance from Evolution API: {}", e.getMessage());
      // Continue with deletion anyway
    }

    instanceRepository.delete(instance);
  }

  /**
   * Regenerate QR code for an existing instance
   * 
   * @param instanceId The ID of the instance
   * @return The QR code response
   */
  @Transactional
  public JsonNode regenerateQrCode(Long instanceId) {
    WhatsappInstance instance = instanceRepository.findById(instanceId)
        .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found with ID: " + instanceId));

    // Check if current user has permission
    String currentUserEmail = userContext.getCurrentUserEmail();
    boolean isAdmin = userContext.hasRole("ROLE_ADMIN");

    if (!isAdmin && (instance.getOwner() == null ||
        !instance.getOwner().getEmail().equals(currentUserEmail))) {
      throw new BusinessException("You don't have permission to regenerate QR for this instance");
    }

    // First disconnect
    whatsappClient.disconnectInstance(instance.getToken(), instance.getInstanceId());

    // Update status
    instance.setStatus(InstanceStatus.CONNECTING);
    instanceRepository.save(instance);

    // Return new QR code
    return whatsappClient.getQrCode(instance.getToken(), instance.getInstanceId());
  }

  /**
   * Send bulk text message to multiple recipients
   * 
   * @param recipients  List of recipient IDs
   * @param messageText The message content
   * @return Map of recipient to success/failure status
   */
  @Transactional
  public void sendBulkTextMessage(List<String> recipients, String messageText) {
    for (String recipient : recipients) {
      try {
        sendTextMessage(recipient, messageText);
        log.info("Successfully sent message to {}", recipient);
      } catch (Exception e) {
        log.error("Failed to send message to {}: {}", recipient, e.getMessage());
        // Continue with next recipient
      }
    }
  }

  /**
   * Send bulk media message to multiple recipients
   * 
   * @param recipients   List of recipient IDs
   * @param mediaRequest The media message request
   */
  @Transactional
  public void sendBulkMediaMessage(List<String> recipients, MediaMessageRequest mediaRequest) {
    for (String recipient : recipients) {
      try {
        sendMediaMessage(recipient, mediaRequest);
        log.info("Successfully sent media to {}", recipient);
      } catch (Exception e) {
        log.error("Failed to send media to {}: {}", recipient, e.getMessage());
        // Continue with next recipient
      }
    }
  }
}