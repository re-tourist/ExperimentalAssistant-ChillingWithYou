package com.experimentalassistant.backend.service.ai.impl;

import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.service.ai.AiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.experimentalassistant.backend.dto.AiAnalysisResponse;

@Service("httpAiProvider")
public class HttpAiProvider implements AiProvider {

    // ... (fields and constructor)
    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    @Autowired
    public HttpAiProvider(AiProperties aiProperties, RestTemplateBuilder builder) {
        this.aiProperties = aiProperties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(aiProperties.getHttp().getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(aiProperties.getHttp().getTimeoutMs()))
                .build();
    }

    @Override
    public AiDraftResponse generate(Map<String, Object> context) {
        // ... (existing code)
        String url = aiProperties.getHttp().getBaseUrl() + "/generate";
        
        // Construct Request Body
        Map<String, Object> requestBody = new HashMap<>();
        String prompt = (String) context.getOrDefault("userHint", "Analyze this run");
        requestBody.put("prompt", prompt);
        requestBody.put("context", context);
        if (aiProperties.getHttp().getModel() != null) {
            requestBody.put("model", aiProperties.getHttp().getModel());
        }

        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.postForObject(url, entity, AiDraftResponse.class);
    }

    @Override
    public AiAnalysisResponse analyze(Map<String, Object> fullContext) {
        // Assume the external AI service supports a generic /analyze endpoint or reuses /generate
        // We will send the pre-constructed prompt
        String url = aiProperties.getHttp().getBaseUrl() + "/analyze"; // Or reuse /generate depending on protocol
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", fullContext.get("prompt")); // The 4-section constructed prompt
        requestBody.put("raw_context", fullContext.get("contextJson")); // Optional: send raw context if needed by AI
        if (aiProperties.getHttp().getModel() != null) {
            requestBody.put("model", aiProperties.getHttp().getModel());
        }

        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.postForObject(url, entity, AiAnalysisResponse.class);
    }
    
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiProperties.getHttp().getApiKey() != null && !aiProperties.getHttp().getApiKey().isEmpty()) {
            headers.set("Authorization", "Bearer " + aiProperties.getHttp().getApiKey());
            headers.set("X-API-KEY", aiProperties.getHttp().getApiKey());
        }
        return headers;
    }
}
