package com.valedosol.kaju.feature.evolution.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
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