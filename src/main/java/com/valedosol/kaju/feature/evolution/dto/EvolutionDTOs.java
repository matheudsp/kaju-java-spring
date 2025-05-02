package com.valedosol.kaju.feature.evolution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EvolutionDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a new Evolution API instance")
    public static class CreateInstanceRequest {
        
        @Schema(description = "Name of the instance to create", example = "my-whatsapp", required = true)
        private String instanceName;
        
        @Schema(description = "Optional API token", example = "a1b2c3d4e5f6g7h8i9j0")
        private String token;
        
        @Schema(description = "Optional phone number to associate with this instance", example = "5511999999999")
        private String number;
        
        @Schema(description = "Whether to generate QR code for connection", example = "true", defaultValue = "true")
        private boolean qrcode = true;
        
        @Schema(description = "Integration type", example = "WHATSAPP-BAILEYS", defaultValue = "WHATSAPP-BAILEYS")
        private String integration = "WHATSAPP-BAILEYS";
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Response from creating an Evolution API instance")
    public static class InstanceResponse {
        
        @Schema(description = "Name of the instance", example = "my-whatsapp")
        private String instanceName;
        
        @Schema(description = "Instance connection status", example = "CONNECTED")
        private String status;
        
        @Schema(description = "API token/hash of the instance", example = "a1b2c3d4e5f6g7h8i9j0")
        private String hash;
        
        @Schema(description = "QR code data in base64 format", example = "data:image/png;base64,...")
        private QRCodeData qrcode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "QR code data")
    public static class QRCodeData {
        
        @Schema(description = "Base64 encoded QR code image", example = "data:image/png;base64,...")
        private String base64;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing connection state")
    public static class ConnectionStateResponse {
        
        @Schema(description = "Connection state", example = "CONNECTED")
        private String state;
        
        @Schema(description = "Detailed status information")
        private String status;
    }
}