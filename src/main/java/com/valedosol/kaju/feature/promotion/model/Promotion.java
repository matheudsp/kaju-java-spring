package com.valedosol.kaju.feature.promotion.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.target.model.Target;

@Entity
@Data
@NoArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account creator;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PromotionTarget> promotionTargets = new ArrayList<>();

    private LocalDateTime scheduledTime;

    private boolean recurring = false;
    private String recurrencePattern = "WEEKLY"; // Por enquanto, apenas suporte semanal
    private Integer recurrenceDayOfWeek; // 1-7 representando o dia da semana (1 = Segunda)
    private LocalDateTime recurrenceEndDate; // Data fim da recorrência (opcional)
    private LocalDateTime nextRecurrence; // Próxima data/hora de envio para recorrência
    private Integer totalOccurrences = 0; // Contador de quantas vezes a promoção foi enviada
    private boolean sent = false;
    // Constructor básico
    public Promotion(String title, String description, String imageUrl,
                     Account creator, LocalDateTime scheduledTime) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.creator = creator;
        this.scheduledTime = scheduledTime;
    }

    // Constructor com recorrência
    public Promotion(String title, String description, String imageUrl,
                     Account creator, LocalDateTime scheduledTime,
                     boolean recurring, Integer recurrenceDayOfWeek, LocalDateTime recurrenceEndDate) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.creator = creator;
        this.scheduledTime = scheduledTime;
        this.recurring = recurring;
        this.recurrenceDayOfWeek = recurrenceDayOfWeek;
        this.recurrenceEndDate = recurrenceEndDate;
        if (recurring) {
            this.nextRecurrence = scheduledTime;
        }
    }

    // Helper method to add a target
    public void addTarget(Target target) {
        PromotionTarget promotionTarget = new PromotionTarget(this, target);
        promotionTargets.add(promotionTarget);
        // Set the promotion reference back
        promotionTarget.setPromotion(this);
    }

    public void addPromotionTarget(PromotionTarget promotionTarget) {
        if (promotionTargets == null) {
            promotionTargets = new ArrayList<>();
        }
        promotionTargets.add(promotionTarget);
        promotionTarget.setPromotion(this);
    }

    // Método para verificar se a promoção foi enviada para todos os destinos
    public boolean isFullySent() {
        if (promotionTargets.isEmpty()) {
            return false;
        }

        return promotionTargets.stream().allMatch(PromotionTarget::isSent);
    }

    // Método para verificar se a promoção foi enviada para pelo menos um destino
    public boolean isPartiallySent() {
        return promotionTargets.stream().anyMatch(PromotionTarget::isSent);
    }

    // Método para calcular a próxima recorrência
    public void calculateNextRecurrence() {
        if (!recurring) return;

        // Se já temos uma data de próxima recorrência, avançamos uma semana
        if (nextRecurrence != null) {
            nextRecurrence = nextRecurrence.plusWeeks(1);
        } else {
            // Se não temos, usamos a data agendada original
            nextRecurrence = scheduledTime.plusWeeks(1);
        }

        // Verificar se já ultrapassamos a data de fim de recorrência
        if (recurrenceEndDate != null && nextRecurrence.isAfter(recurrenceEndDate)) {
            recurring = false;
            nextRecurrence = null;
        }
    }
}