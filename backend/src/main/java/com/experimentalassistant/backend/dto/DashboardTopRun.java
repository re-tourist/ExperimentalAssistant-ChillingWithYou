package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DashboardTopRun {
    private Long runId;
    private String runName;
    private String modelName;
    private String datasetName;
    private Double value;
    private LocalDateTime endTime;
    private String status;
}
