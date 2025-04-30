package com.valedosol.kaju.feature.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.valedosol.kaju.feature.subscription.model.StripeSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, Long> {
    Optional<StripeSubscription> findByAccountIdAndStatus(Long accountId, String status);
    
    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    Optional<StripeSubscription> findByAccountIdAndStatusEquals(Long accountId, String status);
    
    List<StripeSubscription> findByNextBillingDateLessThanEqualAndStatusEquals(LocalDateTime date, String status);
}