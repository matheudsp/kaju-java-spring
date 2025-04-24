package com.valedosol.kaju.repository;

import com.valedosol.kaju.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByCreatorId(Long creatorId);

    // Método para buscar promoções recorrentes de um usuário
    List<Promotion> findByCreatorIdAndRecurringTrue(Long creatorId);

    // Busca promoções agendadas para um intervalo de tempo específico
    List<Promotion> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);

    // Busca promoções que deveriam ter sido enviadas até agora
    List<Promotion> findByScheduledTimeBefore(LocalDateTime endTime);

    // Busca promoções recorrentes com próxima recorrência antes de determinado momento
    List<Promotion> findByRecurringTrueAndNextRecurrenceBefore(LocalDateTime dateTime);


    @Query("SELECT DISTINCT p FROM Promotion p LEFT JOIN FETCH p.promotionTargets pt " +
            "WHERE p.scheduledTime <= :endTime AND (SELECT COUNT(pt2) FROM PromotionTarget pt2 WHERE pt2.promotion = p AND pt2.sent = false) > 0")
    List<Promotion> findByScheduledTimeBeforeAndNotFullySent(@Param("endTime") LocalDateTime endTime);
}