package com.valedosol.kaju.feature.subscriptionPayment.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.valedosol.kaju.feature.subscriptionPayment.model.StripeSubscription;

import java.util.Optional;

public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, Long> {
    Optional<StripeSubscription> findByAccountId(Long accountId);
    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}