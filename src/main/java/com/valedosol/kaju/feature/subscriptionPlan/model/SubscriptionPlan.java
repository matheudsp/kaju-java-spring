package com.valedosol.kaju.feature.subscriptionPlan.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer weeklyAllowedSends;
    private Double price;

    private String stripeProductId;
    private String stripePriceId;
    private String billingInterval = "month"; // "month" or "year"

    // Constructor
    public SubscriptionPlan(String name, Integer weeklyAllowedSends, Double price) {
        this.name = name;
        this.weeklyAllowedSends = weeklyAllowedSends;
        this.price = price;
    }
}