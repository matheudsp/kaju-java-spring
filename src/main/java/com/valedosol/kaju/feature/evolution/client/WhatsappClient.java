package com.valedosol.kaju.feature.evolution.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.valedosol.kaju.feature.evolution.config.WhatsappProperties;
import com.valedosol.kaju.feature.evolution.dto.MediaMessageRequest;
import com.valedosol.kaju.feature.evolution.dto.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
// import reactor.core.publisher.Mono;

// import java.util.Base64;

@Component
@Slf4j
public class WhatsappClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final WhatsappProperties properties;

    public WhatsappClient(WhatsappProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MessageResponse sendText(String token, String instanceId, String to, String messageText) {
        log.info("Sending text message to {} using instance {}", to, instanceId);
        
        ObjectNode requestBody = objectMapper.createObjectNode()
                .put("to", to)
                .put("type", "text");
        
        ObjectNode textNode = objectMapper.createObjectNode()
                .put("body", messageText);
        requestBody.set("text", textNode);

        return webClient.post()
                .uri("/api/v1/message/send")
                .header("Authorization", "Bearer " + token)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .doOnError(e -> log.error("Error sending text message: {}", e.getMessage(), e))
                .block();
    }

    public MessageResponse sendMedia(String token, String instanceId, String to, MediaMessageRequest mediaRequest) {
        log.info("Sending media message to {} using instance {}", to, instanceId);
        
        ObjectNode requestBody = objectMapper.createObjectNode()
                .put("to", to)
                .put("type", mediaRequest.getType());
        
        ObjectNode mediaNode = objectMapper.createObjectNode();
        
        if (mediaRequest.getUrl() != null) {
            mediaNode.put("url", mediaRequest.getUrl());
        } else if (mediaRequest.getBase64() != null) {
            mediaNode.put("base64", mediaRequest.getBase64());
        }
        
        if (mediaRequest.getCaption() != null) {
            mediaNode.put("caption", mediaRequest.getCaption());
        }
        
        if (mediaRequest.getFilename() != null) {
            mediaNode.put("filename", mediaRequest.getFilename());
        }
        
        requestBody.set(mediaRequest.getType(), mediaNode);

        return webClient.post()
                .uri("/api/v1/message/send")
                .header("Authorization", "Bearer " + token)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .doOnError(e -> log.error("Error sending media message: {}", e.getMessage(), e))
                .block();
    }

    public JsonNode getInstanceStatus(String token, String instanceId) {
        log.info("Checking status for instance {}", instanceId);
        
        return webClient.get()
                .uri("/api/v1/instance/status")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error getting instance status: {}", e.getMessage(), e))
                .block();
    }

    public JsonNode disconnectInstance(String token, String instanceId) {
        log.info("Disconnecting instance {}", instanceId);
        
        return webClient.delete()
                .uri("/api/v1/instance/logout")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error disconnecting instance: {}", e.getMessage(), e))
                .block();
    }

    public JsonNode createInstance(String name) {
        log.info("Creating new instance with name {}", name);
        
        ObjectNode requestBody = objectMapper.createObjectNode()
                .put("instanceName", name);
        
        return webClient.post()
                .uri("/api/v1/instance/create")
                .header("Authorization", "Bearer " + properties.getKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error creating instance: {}", e.getMessage(), e))
                .block();
    }

    public JsonNode getQrCode(String token, String instanceId) {
        log.info("Getting QR code for instance {}", instanceId);
        
        return webClient.get()
                .uri("/api/v1/instance/qrcode")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Error getting QR code: {}", e.getMessage(), e))
                .block();
    }
}