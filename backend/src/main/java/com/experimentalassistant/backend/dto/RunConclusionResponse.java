package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class RunConclusionResponse {
    private Long runId;
    private RunNoteResponse conclusion;
}
