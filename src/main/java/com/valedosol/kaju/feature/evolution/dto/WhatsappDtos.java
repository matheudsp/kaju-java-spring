package com.valedosol.kaju.feature.evolution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WhatsappDtos {

}

// Request DTO for media messages
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MediaMessageRequest {
  private String type; // image, video, document, audio
  private String url;
  private String base64;
  private String caption;
  private String filename; // For documents
}

// Response DTO for messages
@Data
@NoArgsConstructor
@AllArgsConstructor
class MessageResponse {
  private boolean success;
  private String message;
  private MessageData data;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MessageData {
    private String key;
    private String id;
  }
}

// DTO for instance creation
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstanceRequest {
  private String instanceName;
}

// DTO for instance status response
@Data
@NoArgsConstructor
@AllArgsConstructor
class InstanceStatusResponse {
  private boolean success;
  private String message;
  private InstanceStatusData data;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InstanceStatusData {
    private String instance;
    private String status;
    private String qrcode;
    private String number;
  }
}