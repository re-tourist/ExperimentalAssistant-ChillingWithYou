package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiDraftResponse {
    private String draftMd;
    private Extracted extracted;

    @Data
    public static class Extracted {
        private List<String> keyFindings;
        private List<String> nextSteps;
    }
}
