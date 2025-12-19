package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class DashboardSummaryResponse {
    private Long totalRuns;
    private Long runsLast7Days;
    private Double successRate;
    private BestMetric bestMetric;

    @Data
    public static class BestMetric {
        private Long metricDefId;
        private String metricName;
        private Double value;
        private Long runId;
    }
}
