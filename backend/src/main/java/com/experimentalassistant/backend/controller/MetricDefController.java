package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.service.MetricDefService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics/defs")
public class MetricDefController {

    private final MetricDefService metricDefService;

    public MetricDefController(MetricDefService metricDefService) {
        this.metricDefService = metricDefService;
    }

    @GetMapping
    public Result<List<MetricDef>> list() {
        return Result.success(metricDefService.list());
    }

    @PostMapping
    public Result<MetricDef> create(@RequestBody MetricDef metricDef) {
        metricDefService.save(metricDef);
        return Result.success(metricDef);
    }
}
