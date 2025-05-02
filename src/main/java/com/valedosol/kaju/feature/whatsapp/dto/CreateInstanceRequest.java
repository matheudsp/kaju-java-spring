package com.valedosol.kaju.feature.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateInstanceRequest {
    @NotBlank(message = "Instance name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Instance name can only contain letters, numbers, underscores and hyphens")
    private String instanceName;
    
    private String phoneNumber;
    
    private boolean qrcode = true;
    
    private String integration = "WHATSAPP-BAILEYS";
}