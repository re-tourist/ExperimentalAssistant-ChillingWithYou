package com.experimentalassistant.backend.controller;

import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.AiDraftRequest;
import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.service.AiAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.experimentalassistant.backend.dto.AiAnalysisRequest;
import com.experimentalassistant.backend.dto.AiAnalysisResponse;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    @Autowired
    private AiAnalysisService aiAnalysisService;

    @PostMapping("/run-draft")
    public Result<AiDraftResponse> generateRunDraft(@RequestBody AiDraftRequest request) {
        if (request.getRunId() == null) {
            return Result.error("runId is required");
        }
        try {
            AiDraftResponse response = aiAnalysisService.generateDraft(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI Draft generation failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/analyze")
    public Result<AiAnalysisResponse> analyze(@RequestBody AiAnalysisRequest request) {
        if (request.getContextJson() == null || request.getContextJson().isEmpty()) {
            return Result.error("contextJson is required");
        }
        try {
            AiAnalysisResponse response = aiAnalysisService.analyze(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI Analysis failed: " + e.getMessage());
        }
    }
}
