package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiAnalysisRequest {
    private String userIntent;
    private Map<String, Object> contextJson;
    
    // contextJson schema:
    // {
    //   "project": { id, name, description, ai_profile },
    //   "dashboard_snapshot": { summary, trend, distribution, top_runs },
    //   "runs": [ { run_id, name, status, tags, metrics, hyperparameters, note } ]
    // }
}
