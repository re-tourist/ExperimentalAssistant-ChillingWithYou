package com.experimentalassistant.backend.dto;

import lombok.Data;

@Data
public class DashboardStatsResult {
    private Long totalRuns;
    private Long runsLast7Days;
    private Long finishedRuns;
}
