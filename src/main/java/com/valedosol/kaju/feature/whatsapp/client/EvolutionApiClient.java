package com.valedosol.kaju.feature.whatsapp.client;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valedosol.kaju.feature.whatsapp.config.EvolutionConfig;
import com.valedosol.kaju.feature.whatsapp.dto.CreateInstanceRequest;
import com.valedosol.kaju.feature.whatsapp.exception.EvolutionApiException;

import java.util.Collections;
import java.util.Map;

@Component
public class EvolutionApiClient {
    private static final Logger logger = LoggerFactory.getLogger(EvolutionApiClient.class);
    
    private final RestTemplate restTemplate;
    private final EvolutionConfig evolutionConfig;
    private final ObjectMapper objectMapper;
    
    public EvolutionApiClient(EvolutionConfig evolutionConfig) {
        this.restTemplate = new RestTemplate();
        this.evolutionConfig = evolutionConfig;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create a new WhatsApp instance
     */
    public Map<String, Object> createInstance(CreateInstanceRequest request) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<CreateInstanceRequest> httpEntity = new HttpEntity<>(request, headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/create";
            logger.info("Creating WhatsApp instance: {}", request.getInstanceName());
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to create instance. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to create WhatsApp instance");
            }
        } catch (RestClientException e) {
            logger.error("REST client error creating instance", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error creating instance", e);
            throw new EvolutionApiException("Unexpected error creating WhatsApp instance", e);
        }
    }
    
    /**
     * Connect to an existing WhatsApp instance
     */
    public Map<String, Object> connectInstance(String instanceName) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/connect/" + instanceName;
            logger.info("Connecting to WhatsApp instance: {}", instanceName);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to connect to instance. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to connect to WhatsApp instance");
            }
        } catch (RestClientException e) {
            logger.error("REST client error connecting to instance", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error connecting to instance", e);
            throw new EvolutionApiException("Unexpected error connecting to WhatsApp instance", e);
        }
    }
    
    /**
     * Get the connection state of an instance
     */
    public Map<String, Object> getConnectionState(String instanceName) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/connectionState/" + instanceName;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to get connection state. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to get connection state");
            }
        } catch (RestClientException e) {
            logger.error("REST client error getting connection state", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting connection state", e);
            throw new EvolutionApiException("Unexpected error getting connection state", e);
        }
    }
    
    /**
     * Restart an instance
     */
    public Map<String, Object> restartInstance(String instanceName) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/restart/" + instanceName;
            logger.info("Restarting WhatsApp instance: {}", instanceName);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to restart instance. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to restart WhatsApp instance");
            }
        } catch (RestClientException e) {
            logger.error("REST client error restarting instance", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error restarting instance", e);
            throw new EvolutionApiException("Unexpected error restarting WhatsApp instance", e);
        }
    }
    
    /**
     * Logout from an instance
     */
    public Map<String, Object> logoutInstance(String instanceName) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/logout/" + instanceName;
            logger.info("Logging out from WhatsApp instance: {}", instanceName);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.DELETE, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to logout from instance. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to logout from WhatsApp instance");
            }
        } catch (RestClientException e) {
            logger.error("REST client error logging out from instance", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error logging out from instance", e);
            throw new EvolutionApiException("Unexpected error logging out from WhatsApp instance", e);
        }
    }
    
    /**
     * Delete an instance
     */
    public Map<String, Object> deleteInstance(String instanceName) {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/delete/" + instanceName;
            logger.info("Deleting WhatsApp instance: {}", instanceName);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.DELETE, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to delete instance. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to delete WhatsApp instance");
            }
        } catch (RestClientException e) {
            logger.error("REST client error deleting instance", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error deleting instance", e);
            throw new EvolutionApiException("Unexpected error deleting WhatsApp instance", e);
        }
    }
    
    /**
     * Fetch all instances
     */
    public Map<String, Object> fetchInstances() {
        try {
            HttpHeaders headers = createHeaders(evolutionConfig.getGlobalApiKey());
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            
            String url = evolutionConfig.getApiUrl() + "/instance/fetchInstances";
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, httpEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            } else {
                logger.error("Failed to fetch instances. Status: {}", response.getStatusCode());
                throw new EvolutionApiException("Failed to fetch WhatsApp instances");
            }
        } catch (RestClientException e) {
            logger.error("REST client error fetching instances", e);
            throw new EvolutionApiException("Error communicating with Evolution API", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching instances", e);
            throw new EvolutionApiException("Unexpected error fetching WhatsApp instances", e);
        }
    }
    
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}