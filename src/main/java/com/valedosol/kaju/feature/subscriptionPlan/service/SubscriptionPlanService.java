package com.valedosol.kaju.feature.subscriptionPlan.service;



import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.valedosol.kaju.feature.subscriptionPlan.model.SubscriptionPlan;
import com.valedosol.kaju.feature.subscriptionPlan.repository.SubscriptionPlanRepository;

import java.util.List;

@Service
public class SubscriptionPlanService {
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }
    
    @Cacheable("plans")
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }
    
    @Cacheable(value = "plans", key = "#id")
    public SubscriptionPlan getPlanById(Long id) {
        return subscriptionPlanRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }
    
    @CacheEvict(value = "plans", allEntries = true)
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }
    
    @CacheEvict(value = "plans", key = "#plan.id")
    public SubscriptionPlan updatePlan(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }
}