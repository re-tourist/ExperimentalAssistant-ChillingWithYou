package com.experimentalassistant.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateUpsertRequest {
    private String name;
    private String domain;
    private String description;
    private String configJson;
    private List<TemplateMetric> metricDefs;
    private List<TemplateTagInfo> tags;

    @Data
    public static class TemplateMetric {
        private Long metricDefId;
        private Boolean isDefault;
        private Integer sortOrder;
    }

    @Data
    public static class TemplateTagInfo {
        private Long tagId;
        private Boolean isDefault;
        private Integer sortOrder;
    }
}
