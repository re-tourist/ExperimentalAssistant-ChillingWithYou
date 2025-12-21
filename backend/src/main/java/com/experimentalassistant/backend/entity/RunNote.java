package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("run_note")
public class RunNote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private String type; // NOTE | CONCLUSION | AI_DRAFT
    private String title;
    private String contentMd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
