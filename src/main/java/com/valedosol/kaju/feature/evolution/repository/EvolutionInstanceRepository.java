package com.valedosol.kaju.feature.evolution.repository;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.evolution.model.EvolutionInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvolutionInstanceRepository extends JpaRepository<EvolutionInstance, Long> {
    
    List<EvolutionInstance> findByOwner(Account owner);
    
    Optional<EvolutionInstance> findByInstanceName(String instanceName);
    
    Optional<EvolutionInstance> findByOwnerAndInstanceName(Account owner, String instanceName);
    
    boolean existsByInstanceName(String instanceName);
}