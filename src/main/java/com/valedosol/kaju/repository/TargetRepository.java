package com.valedosol.kaju.repository;

import com.valedosol.kaju.model.Target;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TargetRepository extends JpaRepository<Target, Long> {
    Optional<Target> findByIdentifier(String identifier);
    List<Target> findByType(String type);
}