package com.valedosol.kaju.feature.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.valedosol.kaju.feature.subscription.model.StripeSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, Long> {
  Optional<StripeSubscription> findByAccountIdAndStatus(Long accountId, String status);

  Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  @Query("SELECT s FROM StripeSubscription s WHERE s.account.id = :accountId AND s.status = 'active'")
  Optional<StripeSubscription> findActiveSubscriptionByAccountId(@Param("accountId") Long accountId);

  @Query("SELECT s FROM StripeSubscription s WHERE s.nextBillingDate <= :date AND s.status = 'active'")
  List<StripeSubscription> findSubscriptionsDueForRenewal(@Param("date") LocalDateTime date);
}