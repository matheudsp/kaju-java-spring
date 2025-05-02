package com.valedosol.kaju.feature.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "evolution")
@Data
public class EvolutionConfig {
    private String apiUrl;
    private ApiKey apiKey;
    private String instance;
    
    @Data
    public static class ApiKey {
        private String global;
    }
    
    public String getGlobalApiKey() {
        return this.apiKey.getGlobal();
    }
}