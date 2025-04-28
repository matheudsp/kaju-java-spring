package com.valedosol.kaju.feature.promotion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.valedosol.kaju.feature.promotion.model.Promotion;
import com.valedosol.kaju.feature.promotion.model.PromotionTarget;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate;

    @Value("${whapi.api.url}")
    private String whapiApiUrl;

    @Value("${whapi.api.token}")
    private String whapiApiToken;

    @Value("${whapi.phone.id}")
    private String whapiPhoneId;

    public WhatsAppService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a promotion to a specific target
     *
     * @param promotion The promotion to send
     * @param promotionTarget The target to send the promotion to
     * @return true if the message was sent successfully, false otherwise
     */
    public boolean sendPromotionToTarget(Promotion promotion, PromotionTarget promotionTarget) {
        try {
            String targetIdentifier = promotionTarget.getTarget().getIdentifier();

            // Create message payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", targetIdentifier);

            // Generate message caption from promotion
            String caption = createMessageCaption(promotion);
            payload.put("caption", caption);

            // Add image URL if available
            if (promotion.getImageUrl() != null && !promotion.getImageUrl().isEmpty()) {
                payload.put("media", promotion.getImageUrl());
            }

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + whapiApiToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Send message to WhatsApp API
            String endpoint = whapiApiUrl + "/messages/image";
            logger.debug("Sending promotion to {}: {}", targetIdentifier, payload);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent promotion {} to target {}",
                        promotion.getId(), promotionTarget.getTarget().getName());
                return true;
            } else {
                logger.error("Failed to send promotion: API returned status code {}",
                        response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            logger.error("Error sending WhatsApp message: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending promotion: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates a formatted message caption from the promotion details
     *
     * @param promotion The promotion to create a caption for
     * @return The formatted message caption
     */
    private String createMessageCaption(Promotion promotion) {
        StringBuilder caption = new StringBuilder();

        // Add title in bold if available
        if (promotion.getTitle() != null && !promotion.getTitle().isEmpty()) {
            caption.append("*").append(promotion.getTitle()).append("*\n\n");
        }

        // Add description if available
        if (promotion.getDescription() != null && !promotion.getDescription().isEmpty()) {
            caption.append(promotion.getDescription());
        }

        return caption.toString();
    }

    /**
     * Sends a text-only message to a target
     *
     * @param targetIdentifier The target identifier (e.g. "120363417811722085@newsletter")
     * @param message The text message to send
     * @return true if successful, false otherwise
     */
    public boolean sendTextMessage(String targetIdentifier, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", targetIdentifier);
            payload.put("text", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + whapiApiToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String endpoint = whapiApiUrl + "/messages/text";

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error sending text message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends a batch of promotions to multiple targets
     *
     * @param promotion The promotion to send
     * @param targets List of promotion targets
     * @return The number of messages successfully sent
     */
    public int sendPromotionToBatch(Promotion promotion, Iterable<PromotionTarget> targets) {
        int successCount = 0;

        for (PromotionTarget target : targets) {
            if (sendPromotionToTarget(promotion, target)) {
                successCount++;
            }
        }

        return successCount;
    }
}