package com.valedosol.kaju.feature.whatsapp.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.valedosol.kaju.common.exception.BusinessException;
import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.config.UserContext;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.whatsapp.client.EvolutionApiClient;
import com.valedosol.kaju.feature.whatsapp.dto.CreateInstanceRequest;
import com.valedosol.kaju.feature.whatsapp.dto.WhatsAppInstanceDTO;
import com.valedosol.kaju.feature.whatsapp.exception.EvolutionApiException;
import com.valedosol.kaju.feature.whatsapp.model.WhatsAppInstance;
import com.valedosol.kaju.feature.whatsapp.model.WhatsAppInstance.InstanceStatus;
import com.valedosol.kaju.feature.whatsapp.repository.WhatsAppInstanceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WhatsAppInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppInstanceService.class);
    
    private final WhatsAppInstanceRepository instanceRepository;
    private final AccountRepository accountRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final UserContext userContext;
    
    public WhatsAppInstanceService(
            WhatsAppInstanceRepository instanceRepository,
            AccountRepository accountRepository, 
            EvolutionApiClient evolutionApiClient,
            UserContext userContext) {
        this.instanceRepository = instanceRepository;
        this.accountRepository = accountRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.userContext = userContext;
    }
    
    /**
     * Create a new WhatsApp instance for the current user
     */
    @Transactional
    public WhatsAppInstanceDTO createInstance(CreateInstanceRequest request) {
        // Get current user
        String email = userContext.getCurrentUserEmail();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if instance name already exists
        if (instanceRepository.existsByInstanceName(request.getInstanceName())) {
            throw new BusinessException("Instance name already exists. Please choose another name.");
        }
        
        // Format instance name to ensure it's unique (add user prefix)
        String instanceNameWithPrefix = formatInstanceName(request.getInstanceName(), account.getId());
        request.setInstanceName(instanceNameWithPrefix);
        
        try {
            // Create instance in Evolution API
            Map<String, Object> response = evolutionApiClient.createInstance(request);
            
            // Extract token from response
            String token = (String) response.get("hash");
            if (token == null) {
                throw new EvolutionApiException("No token returned from Evolution API");
            }
            
            // Create and save instance in our database
            WhatsAppInstance instance = new WhatsAppInstance(
                    instanceNameWithPrefix, 
                    token, 
                    request.getPhoneNumber(), 
                    account
            );
            
            instance.setStatus(InstanceStatus.CONNECTING);
            instance = instanceRepository.save(instance);
            
            // Create DTO response with QR code if available
            WhatsAppInstanceDTO dto = mapToDTO(instance);
            if (response.containsKey("qrcode") && ((Map<?, ?>) response.get("qrcode")).containsKey("base64")) {
                dto.setQrCode((String) ((Map<?, ?>) response.get("qrcode")).get("base64"));
            }
            
            return dto;
        } catch (EvolutionApiException e) {
            logger.error("Evolution API error creating instance", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating instance", e);
            throw new BusinessException("Failed to create WhatsApp instance", e);
        }
    }
    
    /**
     * Get all instances for the current user
     */
    @Cacheable(value = "instances", key = "#account.id")
    public List<WhatsAppInstanceDTO> getUserInstances(Account account) {
        List<WhatsAppInstance> instances = instanceRepository.findByOwnerId(account.getId());
        return instances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get an instance by ID
     */
    @Transactional(readOnly = true)
    public WhatsAppInstanceDTO getInstance(Long instanceId) {
        String email = userContext.getCurrentUserEmail();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.getId())) {
            throw new BusinessException("You don't have permission to access this instance");
        }
        
        // Get the latest connection state from Evolution API
        try {
            Map<String, Object> stateResponse = evolutionApiClient.getConnectionState(instance.getInstanceName());
            updateInstanceStatus(instance, stateResponse);
            instance = instanceRepository.save(instance);
        } catch (Exception e) {
            logger.warn("Couldn't get connection state for instance: {}", instance.getInstanceName(), e);
        }
        
        return mapToDTO(instance);
    }
    
    /**
     * Connect to an instance
     */
    @Transactional
    @CacheEvict(value = "instances", key = "#account.id")
    public WhatsAppInstanceDTO connectInstance(Long instanceId, Account account) {
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.getId())) {
            throw new BusinessException("You don't have permission to access this instance");
        }
        
        try {
            Map<String, Object> response = evolutionApiClient.connectInstance(instance.getInstanceName());
            
            WhatsAppInstanceDTO dto = mapToDTO(instance);
            if (response.containsKey("base64")) {
                dto.setQrCode((String) response.get("base64"));
            }
            
            // Update status to connecting
            instance.setStatus(InstanceStatus.CONNECTING);
            instanceRepository.save(instance);
            
            return dto;
        } catch (Exception e) {
            logger.error("Error connecting to instance: {}", instance.getInstanceName(), e);
            throw new BusinessException("Failed to connect to WhatsApp instance", e);
        }
    }
    
    /**
     * Restart an instance
     */
    @Transactional
    @CacheEvict(value = "instances", key = "#account.id")
    public WhatsAppInstanceDTO restartInstance(Long instanceId, Account account) {
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.getId())) {
            throw new BusinessException("You don't have permission to access this instance");
        }
        
        try {
            Map<String, Object> response = evolutionApiClient.restartInstance(instance.getInstanceName());
            
            WhatsAppInstanceDTO dto = mapToDTO(instance);
            if (response.containsKey("base64")) {
                dto.setQrCode((String) response.get("base64"));
            }
            
            // Update status to connecting
            instance.setStatus(InstanceStatus.CONNECTING);
            instanceRepository.save(instance);
            
            return dto;
        } catch (Exception e) {
            logger.error("Error restarting instance: {}", instance.getInstanceName(), e);
            throw new BusinessException("Failed to restart WhatsApp instance", e);
        }
    }
    
    /**
     * Logout from an instance
     */
    @Transactional
    @CacheEvict(value = "instances", key = "#account.id")
    public void logoutInstance(Long instanceId, Account account) {
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.getId())) {
            throw new BusinessException("You don't have permission to access this instance");
        }
        
        try {
            evolutionApiClient.logoutInstance(instance.getInstanceName());
            
            // Update status
            instance.setStatus(InstanceStatus.DISCONNECTED);
            instance.setLastDisconnectedAt(LocalDateTime.now());
            instanceRepository.save(instance);
        } catch (Exception e) {
            logger.error("Error logging out from instance: {}", instance.getInstanceName(), e);
            throw new BusinessException("Failed to logout from WhatsApp instance", e);
        }
    }
    
    /**
     * Delete an instance
     */
    @Transactional
    @CacheEvict(value = "instances", key = "#account.id")
    public void deleteInstance(Long instanceId, Account account) {
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.getId())) {
            throw new BusinessException("You don't have permission to delete this instance");
        }
        
        try {
            // Delete from Evolution API
            evolutionApiClient.deleteInstance(instance.getInstanceName());
            
            // Delete from our database
            instanceRepository.delete(instance);
        } catch (Exception e) {
            logger.error("Error deleting instance: {}", instance.getInstanceName(), e);
            // Still delete from our database even if Evolution API fails
            instanceRepository.delete(instance);
        }
    }
    
    /**
     * Get connection state of an instance
     */
    @Transactional
    public WhatsAppInstanceDTO getConnectionState(Long instanceId, Account account) {
        WhatsAppInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp instance not found"));
        
        // Check if the instance belongs to the user
        if (!instance.getOwner().getId().equals(account.