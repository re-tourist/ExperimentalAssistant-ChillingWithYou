package com.experimentalassistant.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardFilter {
    private Long projectId;
    private String dateFrom;
    private String dateTo;
    private String status;
    private List<Long> tagIds;

    private Long metricDefId;
    private String direction;
    private String granularity;
    private String distributionBy;
    private Integer limit;
}
