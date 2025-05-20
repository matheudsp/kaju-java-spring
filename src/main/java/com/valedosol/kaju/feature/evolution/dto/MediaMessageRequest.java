package com.valedosol.kaju.feature.evolution.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaMessageRequest {
    private String type; // image, video, document, audio
    private String url;
    private String base64;
    private String caption;
    private String filename; // For documents
}