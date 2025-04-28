package com.valedosol.kaju.feature.target.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valedosol.kaju.feature.target.model.Target;

import java.util.List;
import java.util.Optional;

public interface TargetRepository extends JpaRepository<Target, Long> {
    Optional<Target> findByIdentifier(String identifier);
    
    List<Target> findByType(String type);
    
    List<Target> findByOwnerId(Long accountId);
    
    List<Target> findByOwnerIsNull();
    
    List<Target> findByOwnerIsNullOrOwnerId(Long accountId);
}