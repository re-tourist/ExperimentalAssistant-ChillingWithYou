package com.experimentalassistant.backend.service;

import com.experimentalassistant.backend.dto.ai.AiChatRequest;
import com.experimentalassistant.backend.dto.ai.AiChatResponse;

public interface AiService {
    AiChatResponse chat(AiChatRequest request);
}
