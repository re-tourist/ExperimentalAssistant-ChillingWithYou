package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class AiDraftRequest {
    private Long runId;
    private String userHint;
    private boolean includeMetrics = true;
    private boolean includeTags = true;
    private boolean includeTemplate = true;
    private boolean includeNotes = true;
    private String mode; // e.g., "summary", "analysis"
    private boolean saveDraft = false; // Optional query param or body
}
