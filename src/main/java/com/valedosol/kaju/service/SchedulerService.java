package com.valedosol.kaju.service;

import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.Promotion;
import com.valedosol.kaju.model.PromotionTarget;
import com.valedosol.kaju.repository.AccountRepository;
import com.valedosol.kaju.repository.PromotionRepository;
import com.valedosol.kaju.repository.PromotionTargetRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final PromotionRepository promotionRepository;
    private final PromotionTargetRepository promotionTargetRepository;
    private final AccountRepository accountRepository;
    private final WhatsAppService whatsAppService;

    public SchedulerService(PromotionRepository promotionRepository,
                            PromotionTargetRepository promotionTargetRepository,
                            AccountRepository accountRepository,
                            WhatsAppService whatsAppService) {
        this.promotionRepository = promotionRepository;
        this.promotionTargetRepository = promotionTargetRepository;
        this.accountRepository = accountRepository;
        this.whatsAppService = whatsAppService;
    }

    // Verifica promoções agendadas a cada minuto
    @Scheduled(fixedRate = 60000)
    public void checkScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();

        // Busca promoções que estão programadas até o momento atual e que ainda não foram totalmente enviadas
        List<Promotion> duePromotions = promotionRepository
                .findByScheduledTimeBeforeAndNotFullySent(now);

        processPromotions(duePromotions, now, "agendadas");

        // Verifica também promoções recorrentes que devem ser enviadas agora
        List<Promotion> dueRecurringPromotions = promotionRepository
                .findByRecurringTrueAndNextRecurrenceBefore(now);

        processPromotions(dueRecurringPromotions, now, "recorrentes");
    }

    private void processPromotions(List<Promotion> promotions, LocalDateTime now, String type) {
        if (!promotions.isEmpty()) {
            logger.info("Encontradas {} promoções {} para envio", promotions.size(), type);
        }

        for (Promotion promotion : promotions) {
            try {
                // Processa o envio
                Account creator = promotion.getCreator();

                // Verifica se o usuário ainda tem envios disponíveis
                if (creator.getRemainingWeeklySends() > 0) {
                    // Envia a promoção para todos os destinos não enviados
                    boolean anySuccess = false;
                    int targetsProcessed = 0;

                    for (PromotionTarget promotionTarget : promotion.getPromotionTargets()) {
                        if (!promotionTarget.isSent()) {
                            boolean sent = whatsAppService.sendPromotionToTarget(promotion, promotionTarget);

                            if (sent) {
                                promotionTarget.setSent(true);
                                promotionTarget.setSentTime(LocalDateTime.now());
                                promotionTargetRepository.save(promotionTarget);
                                anySuccess = true;
                                targetsProcessed++;

                                // Decrementa os envios semanais restantes para cada target enviado
                                creator.setRemainingWeeklySends(creator.getRemainingWeeklySends() - 1);
                                if (creator.getRemainingWeeklySends() <= 0) {
                                    // Se acabaram os envios, interrompe o processamento
                                    break;
                                }
                            }
                        }
                    }

                    // Se envio foi bem sucedido para pelo menos um destino
                    if (anySuccess) {
                        // Se for promoção recorrente e todos os destinos foram processados, calcula a próxima recorrência
                        if (promotion.isRecurring() && promotion.isFullySent()) {
                            promotion.calculateNextRecurrence();
                            promotion.setTotalOccurrences(promotion.getTotalOccurrences() + 1);

                            // Se a recorrência terminou
                            if (!promotion.isRecurring()) {
                                logger.info("Promoção recorrente ID {} encerrada após {} ocorrências",
                                        promotion.getId(), promotion.getTotalOccurrences());
                            } else {
                                // Resetar o status de envio dos targets para a próxima recorrência
                                for (PromotionTarget pt : promotion.getPromotionTargets()) {
                                    pt.setSent(false);
                                    pt.setSentTime(null);
                                    promotionTargetRepository.save(pt);
                                }
                            }
                        }

                        promotionRepository.save(promotion);
                        accountRepository.save(creator);

                        logger.info("Promoção ID {} enviada para {} destinos (ocorrência {})",
                                promotion.getId(), targetsProcessed,
                                promotion.getTotalOccurrences());
                    }
                } else {
                    logger.warn("Usuário {} sem envios disponíveis. Promoção ID {} não enviada.",
                            creator.getEmail(), promotion.getId());

                    // Se for recorrente, programar para a próxima semana
                    if (promotion.isRecurring()) {
                        promotion.calculateNextRecurrence();
                        promotionRepository.save(promotion);
                        logger.info("Promoção recorrente ID {} reprogramada para {}",
                                promotion.getId(), promotion.getNextRecurrence());
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao processar promoção agendada: {}", e.getMessage());
            }
        }
    }

    // Reset weekly sends every Monday at 00:01
    @Scheduled(cron = "0 1 0 * * 1")
    public void resetWeeklySends() {
        logger.info("Executando reset semanal de envios");
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            if (account.getSubscriptionPlan() != null) {
                account.setRemainingWeeklySends(account.getSubscriptionPlan().getWeeklyAllowedSends());
                account.setLastResetDate(LocalDateTime.now());
                accountRepository.save(account);
                logger.info("Resets de envios para usuário {}: {}",
                        account.getEmail(), account.getRemainingWeeklySends());
            }
        }
    }
}