package com.experimentalassistant.backend.dto;

import com.experimentalassistant.backend.entity.Template;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateDetailResponse extends Template {
    private List<Field> fields;
    private List<TemplateMetricDetail> metricDefs;
    private List<TemplateTagDetail> tags;

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
    public static class TemplateMetricDetail {
        private Long metricDefId;
        private String name;
        private String displayName;
        private String direction;
        private Boolean isDefault;
        private Integer sortOrder;
    }

    @Data
    public static class TemplateTagDetail {
        private Long tagId;
        private String name;
        private Boolean isDefault;
        private Integer sortOrder;
    }
}
