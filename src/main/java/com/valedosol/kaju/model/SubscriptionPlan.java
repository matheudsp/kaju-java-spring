package com.valedosol.kaju.model;

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

    // Constructor
    public SubscriptionPlan(String name, Integer weeklyAllowedSends, Double price) {
        this.name = name;
        this.weeklyAllowedSends = weeklyAllowedSends;
        this.price = price;
    }
}