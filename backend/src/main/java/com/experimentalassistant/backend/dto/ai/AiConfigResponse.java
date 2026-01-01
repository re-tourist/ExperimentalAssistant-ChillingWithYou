package com.experimentalassistant.backend.dto.ai;

import lombok.Data;

@Data
public class AiConfigResponse {
    private boolean enabled;
    private String provider;
    private String model;
    private String baseUrl;
}

