package com.experimentalassistant.backend.dto;

import com.experimentalassistant.backend.entity.Run;
import com.experimentalassistant.backend.entity.Tag;
import lombok.Data;

import java.util.List;

@Data
public class RunDetailResponse extends Run {
    private List<Tag> tags;
    private List<MetricDetail> metrics;
    private java.util.Map<String, Object> fieldValues;

    @Data
    public static class MetricDetail {
        private Long metricDefId;
        private String name;
        private String displayName; // In this system, name is display name usually, but requirement says name and displayName
        private String direction;
        private Double value;
    }
}
