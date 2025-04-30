package com.valedosol.kaju.feature.subscription.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Objects;

import com.valedosol.kaju.feature.auth.model.Account;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "stripe_subscription", indexes = {
    @Index(name = "idx_stripe_sub_id", columnList = "stripeSubscriptionId"),
    @Index(name = "idx_stripe_account", columnList = "account_id")
})
public class StripeSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    @ToString.Exclude
    private SubscriptionPlan subscriptionPlan;
    
    @Column(nullable = false)
    private String stripeCustomerId;
    
    @Column(nullable = false, unique = true)
    private String stripeSubscriptionId;
    
    private String stripePaymentMethodId;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime nextBillingDate;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StripeSubscription that = (StripeSubscription) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
