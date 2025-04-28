package com.valedosol.kaju.feature.promotion.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.valedosol.kaju.feature.target.model.Target;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class PromotionTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    @JsonBackReference
    private Promotion promotion;

    @ManyToOne
    @JoinColumn(name = "target_id")
    private Target target;

    private boolean sent = false;
    private LocalDateTime sentTime;

    public PromotionTarget(Promotion promotion, Target target) {
        this.target = target;
        this.setPromotion(promotion); // This will establish the bidirectional relationship
    }
}