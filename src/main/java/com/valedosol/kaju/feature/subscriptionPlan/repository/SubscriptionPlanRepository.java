package com.valedosol.kaju.feature.subscriptionPlan.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valedosol.kaju.feature.subscriptionPlan.model.SubscriptionPlan;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    SubscriptionPlan findByName(String name);
}