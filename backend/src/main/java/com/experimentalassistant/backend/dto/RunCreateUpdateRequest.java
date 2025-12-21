package com.experimentalassistant.backend.dto;

import lombok.Data;
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
    private Long templateId;
    private List<Long> tagIds;
    private List<Metric> metrics;

    @Data
    public static class Metric {
        private Long metricDefId;
        private Double value;
    }
}
