package com.valedosol.kaju.feature.target.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.valedosol.kaju.common.exception.ResourceNotFoundException;
import com.valedosol.kaju.feature.target.model.Target;
import com.valedosol.kaju.feature.target.repository.TargetRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TargetService {

    private final TargetRepository targetRepository;

    public TargetService(TargetRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    /**
     * Get all targets with caching - this list is likely small enough to cache
     */
    @Cacheable(value = "targets", key = "'allTargets'")
    public List<Target> getAllTargets() {
        return targetRepository.findAll();
    }

    /**
     * Get target by ID with caching
     */
    @Cacheable(value = "targets", key = "#id")
    public Target getTargetById(Long id) {
        return targetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Target not found with id: " + id));
    }

    /**
     * Get targets by type with caching
     */
    @Cacheable(value = "targets", key = "'type_' + #type")
    public List<Target> getTargetsByType(String type) {
        return targetRepository.findByType(type);
    }

    /**
     * Create a new target - update cache with the new target
     */
    @CachePut(value = "targets", key = "#result.id")
    @CacheEvict(value = "targets", key = "'allTargets'")
    @Transactional
    public Target createTarget(Target target) {
        return targetRepository.save(target);
    }

    /**
     * Update a target - update the cache with new values
     */
    @CachePut(value = "targets", key = "#target.id")
    @CacheEvict(value = "targets", allEntries = true)
    @Transactional
    public Target updateTarget(Target target) {
        if (!targetRepository.existsById(target.getId())) {
            throw new ResourceNotFoundException("Target not found with id: " + target.getId());
        }
        return targetRepository.save(target);
    }

    /**
     * Delete a target - evict it from cache
     */
    @CacheEvict(value = "targets", allEntries = true)
    @Transactional
    public void deleteTarget(Long id) {
        if (!targetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Target not found with id: " + id);
        }
        targetRepository.deleteById(id);
    }

    public Optional<Target> findByWaId(String identifier) {
        return targetRepository.findByIdentifier(identifier);
    }

}