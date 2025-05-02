package com.valedosol.kaju.feature.whatsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.valedosol.kaju.feature.whatsapp.model.WhatsAppInstance;
import com.valedosol.kaju.feature.whatsapp.model.WhatsAppInstance.InstanceStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppInstanceRepository extends JpaRepository<WhatsAppInstance, Long> {
    
    List<WhatsAppInstance> findByOwnerId(Long ownerId);
    
    Optional<WhatsAppInstance> findByInstanceName(String instanceName);
    
    Optional<WhatsAppInstance> findByTokenAndOwnerId(String token, Long ownerId);
    
    Optional<WhatsAppInstance> findByInstanceNameAndOwnerId(String instanceName, Long ownerId);
    
    List<WhatsAppInstance> findByStatus(InstanceStatus status);
    
    Optional<WhatsAppInstance> findByPhoneNumber(String phoneNumber);
    
    boolean existsByInstanceName(String instanceName);
}