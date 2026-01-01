package com.experimentalassistant.backend.controller;

import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.ai.AiChatRequest;
import com.experimentalassistant.backend.dto.ai.AiChatResponse;
import com.experimentalassistant.backend.dto.ai.AiConfigResponse;
import com.experimentalassistant.backend.service.AiService;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiProperties aiProperties;

    public AiController(AiService aiService, AiProperties aiProperties) {
        this.aiService = aiService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/config")
    public Result<AiConfigResponse> config() {
        AiConfigResponse res = new AiConfigResponse();
        boolean enabled = false;
        if (aiProperties.isEnable()) {
            if ("mock".equalsIgnoreCase(aiProperties.getProvider())) {
                enabled = true;
            } else if ("http".equalsIgnoreCase(aiProperties.getProvider())) {
                enabled = aiProperties.getHttp() != null && StringUtils.hasText(aiProperties.getHttp().getApiKey());
            }
        }
        res.setEnabled(enabled);
        res.setProvider(aiProperties.getProvider());
        res.setModel(aiProperties.getHttp() != null ? aiProperties.getHttp().getModel() : null);
        res.setBaseUrl(aiProperties.getHttp() != null ? aiProperties.getHttp().getBaseUrl() : null);
        return Result.success(res);
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        try {
            AiChatResponse response = aiService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            // Check if it's a known error (e.g. AI disabled)
            if ("AI_DISABLED".equals(e.getMessage())) {
                return Result.error(1001, "AI 未启用（部署环境未配置 KEY）");
            }
            return Result.error(e.getMessage());
        }
    }
}
