package com.valedosol.kaju.feature.evolution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valedosol.kaju.common.exception.BusinessException;
import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.config.UserContext;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.service.AccountService;
import com.valedosol.kaju.feature.evolution.config.EvolutionConfig;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.ConnectionStateResponse;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.CreateInstanceRequest;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.InstanceResponse;
import com.valedosol.kaju.feature.evolution.dto.EvolutionDTOs.QRCodeData;
import com.valedosol.kaju.feature.evolution.model.EvolutionInstance;
import com.valedosol.kaju.feature.evolution.model.EvolutionInstance.EvolutionInstanceStatus;
import com.valedosol.kaju.feature.evolution.repository.EvolutionInstanceRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class EvolutionApiService {

    private final EvolutionConfig evolutionConfig;
    private final EvolutionInstanceRepository evolutionInstanceRepository;
    private final AccountService accountService;
    private final UserContext userContext;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EvolutionApiService(
            EvolutionConfig evolutionConfig,
            EvolutionInstanceRepository evolutionInstanceRepository,
            AccountService accountService,
            UserContext userContext,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.evolutionConfig = evolutionConfig;
        this.evolutionInstanceRepository = evolutionInstanceRepository;
        this.accountService = accountService;
        this.userContext = userContext;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new Evolution API instance
     */
    @Transactional
    public InstanceResponse createInstance(CreateInstanceRequest request) {
        String currentUserEmail = userContext.getCurrentUserEmail();
        if (currentUserEmail == null) {
            throw new BusinessException("User must be authenticated to create an instance");
        }

        Account account = accountService.getAccountByEmail(currentUserEmail);

        // Check if instance with this name already exists globally
        if (evolutionInstanceRepository.existsByInstanceName(request.getInstanceName())) {
            throw new BusinessException("An instance with the name '" + request.getInstanceName() + "' already exists");
        }

        try {
            // Call Evolution API to create instance
            String url = evolutionConfig.getApi().getUrl() + "/instance/create";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionConfig.getApi().getKey());
            
            HttpEntity<CreateInstanceRequest> requestEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            
            // Parse the response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            InstanceResponse instanceResponse = new InstanceResponse();
            instanceResponse.setInstanceName(request.getInstanceName());
            
            if (jsonNode.has("hash")) {
                instanceResponse.setHash(jsonNode.get("hash").asText());
            }
            
            if (jsonNode.has("qrcode") && jsonNode.get("qrcode").has("base64")) {
                QRCodeData qrCodeData = new QRCodeData();
                qrCodeData.setBase64(jsonNode.get("qrcode").get("base64").asText());
                instanceResponse.setQrcode(qrCodeData);
                instanceResponse.setStatus(EvolutionInstanceStatus.PENDING_CONNECTION.name());
            } else {
                instanceResponse.setStatus(EvolutionInstanceStatus.CONNECTED.name());
            }
            
            // Save instance information to our database
            EvolutionInstance instance = EvolutionInstance.builder()
                    .instanceName(request.getInstanceName())
                    .apiToken(instanceResponse.getHash())
                    .phoneNumber(request.getNumber())
                    .owner(account)
                    .status(instanceResponse.getQrcode() != null ? 
                            EvolutionInstanceStatus.PENDING_CONNECTION : 
                            EvolutionInstanceStatus.CONNECTED)
                    .build();
            
            evolutionInstanceRepository.save(instance);
            
            return instanceResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("Error creating Evolution instance: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to create Evolution instance: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating Evolution instance", e);
            throw new BusinessException("Failed to create Evolution instance: " + e.getMessage());
        }
    }

    /**
     * Get all instances owned by the current user
     */
    public List<EvolutionInstance> getUserInstances() {
        String currentUserEmail = userContext.getCurrentUserEmail();
        if (currentUserEmail == null) {
            throw new BusinessException("User must be authenticated to list instances");
        }

        Account account = accountService.getAccountByEmail(currentUserEmail);
        return evolutionInstanceRepository.findByOwner(account);
    }

    /**
     * Get connection status of an instance
     */
    public ConnectionStateResponse getConnectionState(String instanceName) {
        validateInstanceOwnership(instanceName);
        
        try {
            String url = evolutionConfig.getApi().getUrl() + "/instance/connectionState/" + instanceName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class);
            
            // Parse the response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            ConnectionStateResponse stateResponse = new ConnectionStateResponse();
            
            if (jsonNode.has("state")) {
                stateResponse.setState(jsonNode.get("state").asText());
            }
            
            if (jsonNode.has("status")) {
                stateResponse.setStatus(jsonNode.get("status").asText());
            }
            
            // Update instance status in our database
            EvolutionInstance instance = getInstanceByName(instanceName);
            EvolutionInstanceStatus newStatus = "CONNECTED".equals(stateResponse.getState()) ?
                    EvolutionInstanceStatus.CONNECTED : EvolutionInstanceStatus.DISCONNECTED;
            
            if (instance.getStatus() != newStatus) {
                instance.setStatus(newStatus);
                evolutionInstanceRepository.save(instance);
            }
            
            return stateResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("Error fetching connection state: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to get connection state: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching connection state", e);
            throw new BusinessException("Failed to get connection state: " + e.getMessage());
        }
    }

    /**
     * Connect to an instance (generate QR code)
     */
    public InstanceResponse connectInstance(String instanceName) {
        validateInstanceOwnership(instanceName);
        
        try {
            String url = evolutionConfig.getApi().getUrl() + "/instance/connect/" + instanceName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class);
            
            // Parse the response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            InstanceResponse instanceResponse = new InstanceResponse();
            instanceResponse.setInstanceName(instanceName);
            
            // Update status based on response
            if (jsonNode.has("base64")) {
                QRCodeData qrCodeData = new QRCodeData();
                qrCodeData.setBase64(jsonNode.get("base64").asText());
                instanceResponse.setQrcode(qrCodeData);
                instanceResponse.setStatus(EvolutionInstanceStatus.PENDING_CONNECTION.name());
                
                // Update status in database
                EvolutionInstance instance = getInstanceByName(instanceName);
                instance.setStatus(EvolutionInstanceStatus.PENDING_CONNECTION);
                evolutionInstanceRepository.save(instance);
            }
            
            return instanceResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("Error connecting to instance: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to connect to instance: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error connecting to instance", e);
            throw new BusinessException("Failed to connect to instance: " + e.getMessage());
        }
    }

    /**
     * Restart an instance
     */
    public InstanceResponse restartInstance(String instanceName) {
        validateInstanceOwnership(instanceName);
        
        try {
            String url = evolutionConfig.getApi().getUrl() + "/instance/restart/" + instanceName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            
            // Parse the response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            InstanceResponse instanceResponse = new InstanceResponse();
            instanceResponse.setInstanceName(instanceName);
            
            // Update status based on response
            if (jsonNode.has("base64")) {
                QRCodeData qrCodeData = new QRCodeData();
                qrCodeData.setBase64(jsonNode.get("base64").asText());
                instanceResponse.setQrcode(qrCodeData);
                instanceResponse.setStatus(EvolutionInstanceStatus.PENDING_CONNECTION.name());
                
                // Update status in database
                EvolutionInstance instance = getInstanceByName(instanceName);
                instance.setStatus(EvolutionInstanceStatus.PENDING_CONNECTION);
                evolutionInstanceRepository.save(instance);
            } else {
                // Assume it's connected or reconnecting
                instanceResponse.setStatus(EvolutionInstanceStatus.CONNECTED.name());
            }
            
            return instanceResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("Error restarting instance: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to restart instance: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error restarting instance", e);
            throw new BusinessException("Failed to restart instance: " + e.getMessage());
        }
    }

    /**
     * Logout from an instance
     */
    @Transactional
    public void logoutInstance(String instanceName) {
        validateInstanceOwnership(instanceName);
        
        try {
            String url = evolutionConfig.getApi().getUrl() + "/instance/logout/" + instanceName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class);
            
            // Update status in database
            EvolutionInstance instance = getInstanceByName(instanceName);
            instance.setStatus(EvolutionInstanceStatus.DISCONNECTED);
            evolutionInstanceRepository.save(instance);
            
        } catch (HttpClientErrorException e) {
            log.error("Error logging out from instance: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to logout from instance: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error logging out from instance", e);
            throw new BusinessException("Failed to logout from instance: " + e.getMessage());
        }
    }

    /**
     * Delete an instance
     */
    @Transactional
    public void deleteInstance(String instanceName) {
        validateInstanceOwnership(instanceName);
        
        try {
            String url = evolutionConfig.getApi().getUrl() + "/instance/delete/" + instanceName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionConfig.getApi().getKey());
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class);
            
            // Delete from our database
            EvolutionInstance instance = getInstanceByName(instanceName);
            evolutionInstanceRepository.delete(instance);
            
        } catch (HttpClientErrorException e) {
            log.error("Error deleting instance: {}", e.getResponseBodyAsString(), e);
            throw new BusinessException("Failed to delete instance: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting instance", e);
            throw new BusinessException("Failed to delete instance: " + e.getMessage());
        }
    }

    /**
     * Helper method to get instance by name and validate ownership
     */
    private EvolutionInstance getInstanceByName(String instanceName) {
        return evolutionInstanceRepository.findByInstanceName(instanceName)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found: " + instanceName));
    }
    
    /**
     * Helper method to validate instance ownership
     */
    private void validateInstanceOwnership(String instanceName) {
        String currentUserEmail = userContext.getCurrentUserEmail();
        if (currentUserEmail == null) {
            throw new BusinessException("User must be authenticated to manage instances");
        }

        Account account = accountService.getAccountByEmail(currentUserEmail);
        
        evolutionInstanceRepository.findByOwnerAndInstanceName(account, instanceName)
                .orElseThrow(() -> new BusinessException("You don't have access to this instance or it doesn't exist"));
    }
}