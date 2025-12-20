package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.service.MetricDefService;
import org.springframework.util.StringUtils;
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
        String name = metricDef == null ? null : metricDef.getName();
        if (!StringUtils.hasText(name)) {
            return Result.error("Metric name is required");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 32) {
            return Result.error("Metric name length must be <= 32");
        }

        String direction = metricDef.getDirection();
        if (!StringUtils.hasText(direction)) {
            metricDef.setDirection("MAX");
        } else {
            String normalizedDirection = direction.trim().toUpperCase();
            if (!"MAX".equals(normalizedDirection) && !"MIN".equals(normalizedDirection)) {
                return Result.error("Invalid direction: " + direction);
            }
            metricDef.setDirection(normalizedDirection);
        }
        metricDef.setName(normalizedName);

        MetricDef existing = metricDefService.getOne(new LambdaQueryWrapper<MetricDef>()
                .apply("LOWER(name) = {0}", normalizedName.toLowerCase()));
        if (existing != null) {
            return Result.success(existing);
        }

        MetricDef toSave = new MetricDef();
        toSave.setName(metricDef.getName());
        toSave.setDirection(metricDef.getDirection());
        toSave.setDescription(metricDef.getDescription());
        try {
            metricDefService.save(toSave);
            return Result.success(toSave);
        } catch (Exception e) {
            MetricDef after = metricDefService.getOne(new LambdaQueryWrapper<MetricDef>()
                    .apply("LOWER(name) = {0}", normalizedName.toLowerCase()));
            if (after != null) {
                return Result.success(after);
            }
            return Result.error(e.getMessage());
        }
    }
}
