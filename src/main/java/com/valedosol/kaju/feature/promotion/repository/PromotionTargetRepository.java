package com.valedosol.kaju.feature.promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valedosol.kaju.feature.promotion.model.PromotionTarget;

import java.util.List;

public interface PromotionTargetRepository extends JpaRepository<PromotionTarget, Long> {
  List<PromotionTarget> findByPromotionId(Long promotionId);

  List<PromotionTarget> findByTargetId(Long targetId);

  List<PromotionTarget> findByPromotionIdAndSent(Long promotionId, boolean sent);

}