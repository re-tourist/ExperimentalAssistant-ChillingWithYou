package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("template_metric_def")
public class TemplateMetricDef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long metricDefId;
    private Boolean isDefault;
    private Integer sortOrder;
}
