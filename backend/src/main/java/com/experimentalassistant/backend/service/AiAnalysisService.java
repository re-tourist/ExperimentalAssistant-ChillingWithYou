package com.experimentalassistant.backend.service;

import com.experimentalassistant.backend.dto.AiDraftRequest;
import com.experimentalassistant.backend.dto.AiDraftResponse;

public interface AiAnalysisService {
    AiDraftResponse generateDraft(AiDraftRequest request);
}
