package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("run")
public class Run {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String name;
    private String status; // RUNNING, FINISHED, FAILED
    private String modelName;
    private String datasetName;
    private String optimizer;
    private Double lr;
    private Integer batchSize;
    private Integer epochs;
    private Integer seed;
    private String note;
    // Template ID removed as per decoupling requirement
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime updatedAt;
}
