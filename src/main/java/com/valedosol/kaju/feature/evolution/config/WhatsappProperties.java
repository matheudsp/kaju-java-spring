package com.valedosol.kaju.feature.evolution.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "evolution.api")
@Data
public class WhatsappProperties {
    private String url;
    private String key;
    private String instance;
}