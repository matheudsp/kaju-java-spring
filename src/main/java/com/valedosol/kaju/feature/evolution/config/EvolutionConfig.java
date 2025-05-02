package com.valedosol.kaju.feature.evolution.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "evolution")
@Data
public class EvolutionConfig {
    
    private Api api = new Api();
    
    @Data
    public static class Api {
        private String url;
        private String key;
    }
}