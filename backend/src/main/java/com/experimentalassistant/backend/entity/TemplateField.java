package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("template_field")
public class TemplateField {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String fieldKey;
    private String label;
    private String fieldType; // TEXT, NUMBER, SELECT, BOOLEAN, TEXTAREA
    private Boolean isRequired;
    private Boolean isGroupBy;
    private String defaultValue;
    private Integer sortOrder;
    private String optionsJson;
    private String unit;
    private String placeholder;
}
