package com.experimentalassistant.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private boolean enable = true; // Default true, but can be disabled
    private String provider = "mock";
    private Http http = new Http();

    @Data
    public static class Http {
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey;
        private long timeoutMs = 90000;
        private String model = "qwen-turbo";
    }
}
