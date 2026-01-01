package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("metric_def")
public class MetricDef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String direction; // MAX | MIN | NONE
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
