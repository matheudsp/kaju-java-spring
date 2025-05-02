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
public class EvolutionService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionService.class);

    private final RestTemplate restTemplate;

    @Value("${evolution.api.url}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key}")
    private String evolutionApiKey;

    @Value("${evolution.instance}")
    private String instance;

    public EvolutionService() {
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
            String targetIdentifier = formatPhoneNumber(promotionTarget.getTarget().getIdentifier());

            // Check if promotion has an image
            if (promotion.getImageUrl() != null && !promotion.getImageUrl().isEmpty()) {
                return sendMediaMessage(targetIdentifier, promotion);
            } else {
                // Send text-only message
                String message = createMessageCaption(promotion);
                return sendTextMessage(targetIdentifier, message);
            }
        } catch (Exception e) {
            logger.error("Unexpected error sending promotion: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends a media message with the promotion details and image
     *
     * @param targetIdentifier The formatted phone number
     * @param promotion The promotion to send
     * @return true if successful, false otherwise
     */
    private boolean sendMediaMessage(String targetIdentifier, Promotion promotion) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("number", targetIdentifier);
            payload.put("mediatype", "image");
            payload.put("mimetype", "image/jpeg"); // Adjust based on your image types
            payload.put("caption", createMessageCaption(promotion));
            payload.put("media", promotion.getImageUrl());
            payload.put("fileName", "promotion.jpg"); // Provide a generic filename

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Using the sendMedia endpoint from Evolution API
            String endpoint = evolutionApiUrl + "/message/sendMedia/" + instance;
            logger.debug("Sending promotion to {}: {}", targetIdentifier, payload);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent promotion media to target {}", targetIdentifier);
                return true;
            } else {
                logger.error("Failed to send promotion media: API returned status code {}", 
                        response.getStatusCode());
                return false;
            }

        } catch (RestClientException e) {
            logger.error("Error sending WhatsApp media message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends a text-only message to a target
     *
     * @param targetIdentifier The target phone number
     * @param message The text message to send
     * @return true if successful, false otherwise
     */
    public boolean sendTextMessage(String targetIdentifier, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("number", targetIdentifier);
            payload.put("text", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Using the sendText endpoint from Evolution API
            String endpoint = evolutionApiUrl + "/message/sendText/" + instance;

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent text message to {}", targetIdentifier);
                return true;
            } else {
                logger.error("Failed to send text message: API returned status code {}", 
                        response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending text message: {}", e.getMessage());
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
     * Formats the phone number to ensure it works with Evolution API
     * This method may need adjustment based on your actual phone number format
     *
     * @param phoneNumber The phone number to format
     * @return The properly formatted phone number
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Remove any potential WhatsApp-specific formatting
        // Evolution API expects numbers in format like: 559999999999
        String formattedNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // If the number has a WhatsApp suffix, remove it
        if (formattedNumber.contains("@")) {
            formattedNumber = formattedNumber.substring(0, formattedNumber.indexOf("@"));
        }
        
        return formattedNumber;
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
            
            // Add a small delay between messages to avoid rate limiting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Batch sending interrupted");
                break;
            }
        }

        return successCount;
    }
}