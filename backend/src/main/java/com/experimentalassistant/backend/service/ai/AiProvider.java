package com.experimentalassistant.backend.service.ai;

import com.experimentalassistant.backend.dto.AiAnalysisResponse;
import com.experimentalassistant.backend.dto.AiDraftResponse;
import java.util.Map;

public interface AiProvider {
    AiDraftResponse generate(Map<String, Object> context);
    
    // New generic analysis method
    default AiAnalysisResponse analyze(Map<String, Object> fullContext) {
         // Default fallback or throw exception if not implemented
         // Ideally, implementations should override this.
         // For backward compatibility, we can leave it or implement basic.
         throw new UnsupportedOperationException("Not implemented yet");
    }
}
