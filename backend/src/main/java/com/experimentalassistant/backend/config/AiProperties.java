package com.experimentalassistant.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String provider = "mock";
    private Http http = new Http();

    @Data
    public static class Http {
        private String baseUrl;
        private String apiKey;
        private long timeoutMs = 5000;
        private String model;
    }
}
