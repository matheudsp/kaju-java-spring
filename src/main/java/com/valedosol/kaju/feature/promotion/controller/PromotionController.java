package com.valedosol.kaju.feature.promotion.controller;

import com.valedosol.kaju.feature.auth.model.Account;
import com.valedosol.kaju.feature.auth.repository.AccountRepository;
import com.valedosol.kaju.feature.promotion.model.Promotion;
import com.valedosol.kaju.feature.promotion.model.PromotionTarget;
import com.valedosol.kaju.feature.promotion.repository.PromotionRepository;
import com.valedosol.kaju.feature.promotion.repository.PromotionTargetRepository;
import com.valedosol.kaju.feature.promotion.service.WhatsAppService;
import com.valedosol.kaju.feature.target.model.Target;
import com.valedosol.kaju.feature.target.repository.TargetRepository;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionRepository promotionRepository;
    private final PromotionTargetRepository promotionTargetRepository;
    private final AccountRepository accountRepository;
    private final TargetRepository targetRepository;
    private final WhatsAppService whatsAppService;

    public PromotionController(PromotionRepository promotionRepository,
                               PromotionTargetRepository promotionTargetRepository,
                               AccountRepository accountRepository,
                               TargetRepository targetRepository,
                               WhatsAppService whatsAppService) {
        this.promotionRepository = promotionRepository;
        this.promotionTargetRepository = promotionTargetRepository;
        this.accountRepository = accountRepository;
        this.targetRepository = targetRepository;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();
        List<Promotion> promotions = promotionRepository.findByCreatorId(account.getId());
        return new ResponseEntity<>(promotions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPromotionById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

        Optional<Promotion> promotionOpt = promotionRepository.findById(id);
        if (!promotionOpt.isPresent()) {
            return new ResponseEntity<>("Promoção não encontrada", HttpStatus.NOT_FOUND);
        }

        Promotion promotion = promotionOpt.get();
        if (promotion.getCreator().getId() != accountOpt.get().getId()) {
            return new ResponseEntity<>("Não autorizado", HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(promotion, HttpStatus.OK);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createPromotion(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();

        // Verifica assinatura e envios restantes
        if (account.getSubscriptionPlan() == null) {
            return new ResponseEntity<>("Sem assinatura ativa", HttpStatus.FORBIDDEN);
        }

        if (account.getRemainingWeeklySends() <= 0) {
            return new ResponseEntity<>("Limite de envios semanais atingido", HttpStatus.FORBIDDEN);
        }

        try {
            // Extrair dados da promoção
            Promotion promotion = new Promotion();
            promotion.setTitle((String) request.get("title"));
            promotion.setDescription((String) request.get("description"));
            promotion.setImageUrl((String) request.get("imageUrl"));
            promotion.setCreator(account);

            // Configurar agendamento
            String scheduledTimeStr = (String) request.get("scheduledTime");
            LocalDateTime scheduledTime = scheduledTimeStr != null ?
                    LocalDateTime.parse(scheduledTimeStr) : null;
            promotion.setScheduledTime(scheduledTime);

            // Configurar recorrência
            Boolean recurring = (Boolean) request.get("recurring");
            promotion.setRecurring(recurring != null && recurring);

            if (promotion.isRecurring()) {
                Integer recurrenceDayOfWeek = (Integer) request.get("recurrenceDayOfWeek");
                if (recurrenceDayOfWeek != null) {
                    promotion.setRecurrenceDayOfWeek(recurrenceDayOfWeek);
                } else if (scheduledTime != null) {
                    promotion.setRecurrenceDayOfWeek(scheduledTime.getDayOfWeek().getValue());
                }

                String recurrenceEndDateStr = (String) request.get("recurrenceEndDate");
                if (recurrenceEndDateStr != null) {
                    promotion.setRecurrenceEndDate(LocalDateTime.parse(recurrenceEndDateStr));
                }

                // Inicializar nextRecurrence
                if (scheduledTime != null) {
                    promotion.setNextRecurrence(scheduledTime);
                }
            }

            // Process the targets first to validate them
            List<Integer> targetIds = (List<Integer>) request.get("targetIds");
            if (targetIds == null || targetIds.isEmpty()) {
                return new ResponseEntity<>("É necessário definir pelo menos um destino para a promoção",
                        HttpStatus.BAD_REQUEST);
            }

            // Validate that at least one target exists
            boolean hasValidTarget = false;
            for (Integer targetId : targetIds) {
                Optional<Target> targetOpt = targetRepository.findById(targetId.longValue());
                if (targetOpt.isPresent()) {
                    hasValidTarget = true;
                    break;
                }
            }

            if (!hasValidTarget) {
                return new ResponseEntity<>("Nenhum destino válido fornecido", HttpStatus.BAD_REQUEST);
            }

            // Save the promotion first to get the ID and make it persistent
            Promotion savedPromotion = promotionRepository.save(promotion);

            // Initialize the list if null
            if (savedPromotion.getPromotionTargets() == null) {
                savedPromotion.setPromotionTargets(new ArrayList<>());
            }

            // Create and persist PromotionTarget objects
            for (Integer targetId : targetIds) {
                Optional<Target> targetOpt = targetRepository.findById(targetId.longValue());
                if (targetOpt.isPresent()) {
                    // Create new PromotionTarget
                    PromotionTarget promotionTarget = new PromotionTarget();
                    promotionTarget.setPromotion(savedPromotion);
                    promotionTarget.setTarget(targetOpt.get());

                    // Save the PromotionTarget
                    promotionTargetRepository.save(promotionTarget);
                }
            }

            // Reload the promotion to get fresh state with targets
            savedPromotion = promotionRepository.findById(savedPromotion.getId()).get();

            // If no scheduled time, send immediately
            if (savedPromotion.getScheduledTime() == null) {
                savedPromotion.setScheduledTime(LocalDateTime.now());

                int targetsProcessed = 0;
                for (PromotionTarget target : savedPromotion.getPromotionTargets()) {
                    if (account.getRemainingWeeklySends() > 0) {
                        boolean sent = whatsAppService.sendPromotionToTarget(savedPromotion, target);

                        if (sent) {
                            target.setSent(true);
                            target.setSentTime(LocalDateTime.now());
                            promotionTargetRepository.save(target);

                            account.setRemainingWeeklySends(account.getRemainingWeeklySends() - 1);
                            accountRepository.save(account);

                            targetsProcessed++;
                        }
                    } else {
                        break;
                    }
                }

                // Se for recorrente e todos foram enviados, calcular próxima recorrência
                if (savedPromotion.isRecurring() && savedPromotion.isFullySent()) {
                    savedPromotion.calculateNextRecurrence();
                    savedPromotion.setTotalOccurrences(savedPromotion.getTotalOccurrences() + 1);
                }

                promotionRepository.save(savedPromotion);

                return new ResponseEntity<>(
                        Map.of(
                                "message", "Promoção enviada para " + targetsProcessed + " destinos",
                                "promotion", savedPromotion
                        ),
                        HttpStatus.OK
                );
            } else {
                // Validação da data de agendamento
                LocalDateTime now = LocalDateTime.now();
                if (savedPromotion.getScheduledTime().isBefore(now)) {
                    return new ResponseEntity<>("Data de agendamento não pode ser no passado",
                            HttpStatus.BAD_REQUEST);
                }

                String message = savedPromotion.isRecurring() ?
                        "Promoção recorrente agendada com sucesso" :
                        "Promoção agendada com sucesso";

                return new ResponseEntity<>(
                        Map.of(
                                "message", message,
                                "scheduledTime", savedPromotion.getScheduledTime(),
                                "recurring", savedPromotion.isRecurring(),
                                "promotion", savedPromotion
                        ),
                        HttpStatus.CREATED
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao processar promoção: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/send-now")
    @Transactional
    public ResponseEntity<?> sendPromotionNow(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();

        // Verifica assinatura e envios restantes
        if (account.getSubscriptionPlan() == null) {
            return new ResponseEntity<>("Sem assinatura ativa", HttpStatus.FORBIDDEN);
        }

        if (account.getRemainingWeeklySends() <= 0) {
            return new ResponseEntity<>("Limite de envios semanais atingido", HttpStatus.FORBIDDEN);
        }

        try {
            // Extrair dados da promoção
            Promotion promotion = new Promotion();
            promotion.setTitle((String) request.get("title"));
            promotion.setDescription((String) request.get("description"));
            promotion.setImageUrl((String) request.get("imageUrl"));
            promotion.setCreator(account);
            promotion.setScheduledTime(LocalDateTime.now());

            // Configurar recorrência
            Boolean recurring = (Boolean) request.get("recurring");
            promotion.setRecurring(recurring != null && recurring);

            if (promotion.isRecurring()) {
                Integer recurrenceDayOfWeek = (Integer) request.get("recurrenceDayOfWeek");
                if (recurrenceDayOfWeek != null) {
                    promotion.setRecurrenceDayOfWeek(recurrenceDayOfWeek);
                } else {
                    promotion.setRecurrenceDayOfWeek(LocalDateTime.now().getDayOfWeek().getValue());
                }

                String recurrenceEndDateStr = (String) request.get("recurrenceEndDate");
                if (recurrenceEndDateStr != null) {
                    promotion.setRecurrenceEndDate(LocalDateTime.parse(recurrenceEndDateStr));
                }

                promotion.setNextRecurrence(LocalDateTime.now());
            }

            // Process the targets first to validate them
            List<Integer> targetIds = (List<Integer>) request.get("targetIds");
            if (targetIds == null || targetIds.isEmpty()) {
                return new ResponseEntity<>("É necessário definir pelo menos um destino para a promoção",
                        HttpStatus.BAD_REQUEST);
            }

            // Validate that at least one target exists
            boolean hasValidTarget = false;
            for (Integer targetId : targetIds) {
                Optional<Target> targetOpt = targetRepository.findById(targetId.longValue());
                if (targetOpt.isPresent()) {
                    hasValidTarget = true;
                    break;
                }
            }

            if (!hasValidTarget) {
                return new ResponseEntity<>("Nenhum destino válido fornecido", HttpStatus.BAD_REQUEST);
            }

            // Salvar a promoção primeiro para obter o ID
            Promotion savedPromotion = promotionRepository.save(promotion);

// Initialize the list if null
            if (savedPromotion.getPromotionTargets() == null) {
                savedPromotion.setPromotionTargets(new ArrayList<>());
            }

// Add the targets
            for (Integer targetId : targetIds) {
                Optional<Target> targetOpt = targetRepository.findById(targetId.longValue());
                if (targetOpt.isPresent()) {
                    // Create the relationship properly
                    PromotionTarget promotionTarget = new PromotionTarget();
                    promotionTarget.setPromotion(savedPromotion); // Set promotion reference
                    promotionTarget.setTarget(targetOpt.get());   // Set target reference

                    // Add to the promotion's collection
                    savedPromotion.getPromotionTargets().add(promotionTarget);

                    // Save the promotion target
                    promotionTargetRepository.save(promotionTarget);
                }
            }

// Save the promotion again with all its relationships
            savedPromotion = promotionRepository.save(savedPromotion);

            // Recarregar a promoção com os targets
            savedPromotion = promotionRepository.findById(savedPromotion.getId()).get();

            // Validar que pelo menos um target foi adicionado
            if (savedPromotion.getPromotionTargets().isEmpty()) {
                promotionRepository.delete(savedPromotion);
                return new ResponseEntity<>("Nenhum destino válido fornecido", HttpStatus.BAD_REQUEST);
            }

            // Enviar para todos os targets imediatamente
            int targetsProcessed = 0;
            for (PromotionTarget target : savedPromotion.getPromotionTargets()) {
                if (account.getRemainingWeeklySends() > 0) {
                    boolean sent = whatsAppService.sendPromotionToTarget(savedPromotion, target);

                    if (sent) {
                        target.setSent(true);
                        target.setSentTime(LocalDateTime.now());
                        promotionTargetRepository.save(target);

                        account.setRemainingWeeklySends(account.getRemainingWeeklySends() - 1);
                        accountRepository.save(account);

                        targetsProcessed++;
                    }
                } else {
                    break;
                }
            }

            // Se for recorrente e todos foram enviados, calcular próxima recorrência
            if (savedPromotion.isRecurring() && savedPromotion.isFullySent()) {
                savedPromotion.calculateNextRecurrence();
                savedPromotion.setTotalOccurrences(savedPromotion.getTotalOccurrences() + 1);
            }

            promotionRepository.save(savedPromotion);

            return new ResponseEntity<>(
                    Map.of(
                            "message", "Promoção enviada para " + targetsProcessed + " destinos",
                            "promotion", savedPromotion
                    ),
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao processar promoção: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updatePromotion(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

        Optional<Promotion> promotionOpt = promotionRepository.findById(id);
        if (!promotionOpt.isPresent()) {
            return new ResponseEntity<>("Promoção não encontrada", HttpStatus.NOT_FOUND);
        }

        Promotion promotion = promotionOpt.get();

        // Verificar se o usuário é o criador da promoção
        if (promotion.getCreator().getId() != accountOpt.get().getId()) {
            return new ResponseEntity<>("Não autorizado", HttpStatus.FORBIDDEN);
        }

        // Verificar se a promoção já foi enviada
        if (promotion.isPartiallySent()) {
            return new ResponseEntity<>("Não é possível editar uma promoção que já foi enviada",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            // Atualizar os campos
            if (request.containsKey("title")) {
                promotion.setTitle((String) request.get("title"));
            }

            if (request.containsKey("description")) {
                promotion.setDescription((String) request.get("description"));
            }

            if (request.containsKey("imageUrl")) {
                promotion.setImageUrl((String) request.get("imageUrl"));
            }

            // Atualizar agendamento
            if (request.containsKey("scheduledTime")) {
                String scheduledTimeStr = (String) request.get("scheduledTime");
                LocalDateTime scheduledTime = scheduledTimeStr != null ?
                        LocalDateTime.parse(scheduledTimeStr) : null;

                // Validar data de agendamento
                if (scheduledTime != null && scheduledTime.isBefore(LocalDateTime.now())) {
                    return new ResponseEntity<>("Data de agendamento não pode ser no passado",
                            HttpStatus.BAD_REQUEST);
                }

                promotion.setScheduledTime(scheduledTime);
            }

            // Atualizar recorrência
            if (request.containsKey("recurring")) {
                Boolean recurring = (Boolean) request.get("recurring");
                promotion.setRecurring(recurring != null && recurring);
            }

            if (promotion.isRecurring()) {
                if (request.containsKey("recurrenceDayOfWeek")) {
                    promotion.setRecurrenceDayOfWeek((Integer) request.get("recurrenceDayOfWeek"));
                }

                if (request.containsKey("recurrenceEndDate")) {
                    String recurrenceEndDateStr = (String) request.get("recurrenceEndDate");
                    if (recurrenceEndDateStr != null) {
                        promotion.setRecurrenceEndDate(LocalDateTime.parse(recurrenceEndDateStr));
                    } else {
                        promotion.setRecurrenceEndDate(null);
                    }
                }

                // Recalcular nextRecurrence se necessário
                if (promotion.getNextRecurrence() == null && promotion.getScheduledTime() != null) {
                    promotion.setNextRecurrence(promotion.getScheduledTime());
                }
            } else {
                promotion.setNextRecurrence(null);
                promotion.setRecurrenceDayOfWeek(null);
                promotion.setRecurrenceEndDate(null);
            }

            // Salvar as alterações
            Promotion savedPromotion = promotionRepository.save(promotion);

            // Atualizar targets se fornecidos
            if (request.containsKey("targetIds")) {
                List<Integer> targetIds = (List<Integer>) request.get("targetIds");

                // Remover targets existentes
                promotionTargetRepository.deleteAll(promotion.getPromotionTargets());
                promotion.getPromotionTargets().clear();

                // Salvar novamente para refletir a remoção dos targets
                promotionRepository.save(promotion);

                // Adicionar novos targets
                for (Integer targetId : targetIds) {
                    Optional<Target> targetOpt = targetRepository.findById(targetId.longValue());
                    if (targetOpt.isPresent()) {
                        PromotionTarget promotionTarget = new PromotionTarget();
                        promotionTarget.setPromotion(savedPromotion);
                        promotionTarget.setTarget(targetOpt.get());
                        promotionTargetRepository.save(promotionTarget);
                    }
                }

                // Reload to get fresh state with new targets
                savedPromotion = promotionRepository.findById(savedPromotion.getId()).get();

                // Verificar se pelo menos um target foi adicionado
                if (savedPromotion.getPromotionTargets().isEmpty()) {
                    return new ResponseEntity<>("Nenhum destino válido fornecido", HttpStatus.BAD_REQUEST);
                }
            }

            return new ResponseEntity<>(savedPromotion, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao atualizar promoção: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePromotion(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

        Optional<Promotion> promotionOpt = promotionRepository.findById(id);
        if (!promotionOpt.isPresent()) {
            return new ResponseEntity<>("Promoção não encontrada", HttpStatus.NOT_FOUND);
        }

        Promotion promotion = promotionOpt.get();

        // Verificar se o usuário é o criador da promoção
        if (promotion.getCreator().getId() != accountOpt.get().getId()) {
            return new ResponseEntity<>("Não autorizado", HttpStatus.FORBIDDEN);
        }

        // Verificar se a promoção já foi enviada
        if (promotion.isPartiallySent()) {
            return new ResponseEntity<>("Não é possível excluir uma promoção que já foi enviada",
                    HttpStatus.BAD_REQUEST);
        }

        promotionRepository.delete(promotion);
        return new ResponseEntity<>("Promoção excluída com sucesso", HttpStatus.OK);
    }

    @GetMapping("/recurring")
    public ResponseEntity<List<Promotion>> getRecurringPromotions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (!accountOpt.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Account account = accountOpt.get();
        List<Promotion> promotions = promotionRepository.findByCreatorIdAndRecurringTrue(account.getId());
        return new ResponseEntity<>(promotions, HttpStatus.OK);
    }
}