package com.experimentalassistant.backend.service.ai;

import com.experimentalassistant.backend.dto.AiDraftResponse;
import java.util.Map;

public interface AiProvider {
    AiDraftResponse generate(Map<String, Object> context);
}
