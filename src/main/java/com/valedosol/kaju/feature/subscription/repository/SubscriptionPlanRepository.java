package com.valedosol.kaju.feature.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.valedosol.kaju.feature.subscription.model.SubscriptionPlan;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByName(String name);
    
    @Query("SELECT p FROM SubscriptionPlan p ORDER BY p.price ASC")
    List<SubscriptionPlan> findAllOrderByPriceAsc();
}
