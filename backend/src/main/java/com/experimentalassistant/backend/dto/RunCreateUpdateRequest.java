package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RunCreateUpdateRequest {
    private Long projectId;
    private String name;
    private String status;
    private String modelName;
    private String datasetName;
    private String optimizer;
    private Double lr;
    private Integer batchSize;
    private Integer epochs;
    private Integer seed;
    private String note;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Long> tagIds;
    private List<Metric> metrics;
    private java.util.Map<String, Object> fieldValues;

    @Data
    public static class Metric {
        private Long metricDefId;
        private Double value;
    }
}
