package com.valedosol.kaju.feature.whatsapp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WhatsAppSchedulerService {

    private final WhatsAppInstanceService whatsAppInstanceService;
    
    public WhatsAppSchedulerService(WhatsAppInstanceService whatsAppInstanceService) {
        this.whatsAppInstanceService = whatsAppInstanceService;
    }
    
    /**
     * Update all instances status every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    public void updateAllInstancesStatus() {
        log.info("Running scheduled task to update all WhatsApp instances status");
        try {
            whatsAppInstanceService.updateAllInstancesStatus();
        } catch (Exception e) {
            log.error("Error updating instances status in scheduled task", e);
        }
    }
}