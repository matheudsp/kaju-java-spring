package com.valedosol.kaju.feature.evolution.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceStatusResponse {
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