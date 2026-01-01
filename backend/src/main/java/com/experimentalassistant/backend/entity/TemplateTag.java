package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("template_tag")
public class TemplateTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long tagId;
    private Boolean isDefault;
    private Integer sortOrder;
}
