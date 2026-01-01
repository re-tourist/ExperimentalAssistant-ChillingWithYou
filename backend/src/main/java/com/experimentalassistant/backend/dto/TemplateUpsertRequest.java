package com.experimentalassistant.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateUpsertRequest {
    private String name;
    private String domain;
    private String description;
    private String configJson;
    private List<Field> fields;
    private List<TemplateMetric> metricDefs;
    private List<TemplateTagInfo> tags;

    @Data
    public static class Field {
        private String fieldKey;
        private String label;
        private String fieldType;
        private Boolean isRequired;
        private Boolean isGroupBy;
        private String defaultValue;
        private Integer sortOrder;
        private String optionsJson;
        private String unit;
        private String placeholder;
    }

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
