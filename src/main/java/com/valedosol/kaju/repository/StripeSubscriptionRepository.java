package com.valedosol.kaju.repository;

import com.valedosol.kaju.model.StripeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, Long> {
    Optional<StripeSubscription> findByAccountId(Long accountId);
    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}