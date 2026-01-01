package com.experimentalassistant.backend.dto.ai;

import lombok.Data;

@Data
public class AiChatResponse {
    private String reply;
    private Usage usage;
    private String provider;
    private String model;

    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}
