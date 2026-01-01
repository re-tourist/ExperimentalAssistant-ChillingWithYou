package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("run_field_value")
public class RunFieldValue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private String fieldKey;
    private String valueText;
}
