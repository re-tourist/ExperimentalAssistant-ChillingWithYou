package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("run_metric")
public class RunMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long metricDefId;
    @TableField("metric_value")
    private Double value;
}
