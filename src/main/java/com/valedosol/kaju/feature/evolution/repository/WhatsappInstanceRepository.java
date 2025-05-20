package com.valedosol.kaju.feature.evolution.repository;

import com.valedosol.kaju.feature.evolution.model.WhatsappInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsappInstanceRepository extends JpaRepository<WhatsappInstance, Long> {
    
    Optional<WhatsappInstance> findByInstanceId(String instanceId);
    
    @Query("SELECT w FROM WhatsappInstance w WHERE w.owner.id = :accountId")
    List<WhatsappInstance> findByOwnerId(Long accountId);
    
    @Query("SELECT w FROM WhatsappInstance w WHERE w.owner.id = :accountId AND w.status = 'CONNECTED'")
    Optional<WhatsappInstance> findActiveInstanceByOwnerId(Long accountId);
    
    @Query("SELECT w FROM WhatsappInstance w WHERE w.isGlobal = true AND w.status = 'CONNECTED'")
    Optional<WhatsappInstance> findGlobalInstance();
    
    boolean existsByInstanceId(String instanceId);
}