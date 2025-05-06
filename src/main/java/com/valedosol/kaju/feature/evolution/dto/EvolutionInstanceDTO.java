package com.valedosol.kaju.feature.evolution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for WhatsApp instances")
public class EvolutionInstanceDTO {
    
    @Schema(description = "Instance ID", example = "1")
    private Long id;
    
    @Schema(description = "Instance name", example = "my-whatsapp")
    private String instanceName;
    
    @Schema(description = "Phone number associated with this instance", example = "5511999999999")
    private String phoneNumber;
    
    @Schema(description = "Current status of the instance", example = "CONNECTED")
    private String status;
    
    @Schema(description = "When the instance was created", example = "2023-05-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the instance was last connected", example = "2023-05-15T12:45:00")
    private LocalDateTime lastConnected;
}