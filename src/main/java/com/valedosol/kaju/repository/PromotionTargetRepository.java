package com.valedosol.kaju.repository;

import com.valedosol.kaju.model.PromotionTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionTargetRepository extends JpaRepository<PromotionTarget, Long> {
    List<PromotionTarget> findByPromotionId(Long promotionId);
    List<PromotionTarget> findByTargetId(Long targetId);
    List<PromotionTarget> findByPromotionIdAndSent(Long promotionId, boolean sent);

}