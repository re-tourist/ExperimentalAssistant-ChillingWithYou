package com.experimentalassistant.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("run_tag")
public class RunTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long tagId;
}
