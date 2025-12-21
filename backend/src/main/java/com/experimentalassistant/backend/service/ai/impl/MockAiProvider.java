package com.experimentalassistant.backend.service.ai.impl;

import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.service.ai.AiProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("mockAiProvider")
public class MockAiProvider implements AiProvider {

    @Override
    public AiDraftResponse generate(Map<String, Object> context) {
        AiDraftResponse response = new AiDraftResponse();
        
        // Mock generation logic based on context
        StringBuilder md = new StringBuilder();
        md.append("# Run Analysis (AI Draft)\n\n");
        md.append("Based on the provided metrics and logs, here is a preliminary analysis:\n\n");
        
        Map<String, Object> run = (Map<String, Object>) context.get("run");
        if (run != null) {
            md.append("- **Run Name**: ").append(run.get("name")).append("\n");
            md.append("- **Status**: ").append(run.get("status")).append("\n");
        }
        
        md.append("\n## Key Observations\n");
        md.append("- The training process shows stable convergence.\n");
        md.append("- No immediate signs of overfitting were detected, though validation loss should be monitored.\n");
        
        // Check for keywords in notes if available
        List<String> notes = (List<String>) context.get("notes");
        if (notes != null && !notes.isEmpty()) {
            boolean hasOOM = notes.stream().anyMatch(n -> n.toLowerCase().contains("oom"));
            if (hasOOM) {
                md.append("- **Warning**: Out of Memory (OOM) events were referenced in the notes.\n");
            }
        }

        md.append("\n## Suggested Next Steps\n");
        md.append("1. Verify the learning rate schedule.\n");
        md.append("2. Consider increasing batch size if memory permits.\n");
        
        response.setDraftMd(md.toString());
        
        AiDraftResponse.Extracted extracted = new AiDraftResponse.Extracted();
        extracted.setKeyFindings(Arrays.asList("Stable convergence", "Potential resource constraints"));
        extracted.setNextSteps(Arrays.asList("Check LR schedule", "Increase batch size"));
        response.setExtracted(extracted);
        
        return response;
    }
}
