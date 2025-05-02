package com.valedosol.kaju.feature.whatsapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

import com.valedosol.kaju.feature.whatsapp.model.WhatsAppInstance.InstanceStatus;

@Data
public class WhatsAppInstanceDTO {
  private Long id;
  private String instanceName;
  private String phoneNumber;
  private InstanceStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime lastConnectedAt;
  private String qrCode;

  // Hide sensitive information
  private boolean connected;
}