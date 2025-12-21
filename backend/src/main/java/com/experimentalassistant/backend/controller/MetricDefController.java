package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.entity.RunMetric;
import com.experimentalassistant.backend.entity.TemplateMetricDef;
import com.experimentalassistant.backend.mapper.RunMetricMapper;
import com.experimentalassistant.backend.mapper.TemplateMetricDefMapper;
import com.experimentalassistant.backend.service.MetricDefService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/metrics/defs")
public class MetricDefController {

    private final MetricDefService metricDefService;
    private final RunMetricMapper runMetricMapper;
    private final TemplateMetricDefMapper templateMetricDefMapper;

    public MetricDefController(MetricDefService metricDefService, RunMetricMapper runMetricMapper, TemplateMetricDefMapper templateMetricDefMapper) {
        this.metricDefService = metricDefService;
        this.runMetricMapper = runMetricMapper;
        this.templateMetricDefMapper = templateMetricDefMapper;
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

    @PutMapping("/{id}")
    public Result<MetricDef> update(@PathVariable Long id, @RequestBody MetricDef metricDef) {
        try {
            MetricDef existing = metricDefService.getById(id);
            if (existing == null) {
                return Result.error("Metric definition not found");
            }

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
                direction = "MAX";
            } else {
                direction = direction.trim().toUpperCase();
                if (!"MAX".equals(direction) && !"MIN".equals(direction)) {
                    return Result.error("Invalid direction: " + metricDef.getDirection());
                }
            }

            MetricDef sameName = metricDefService.getOne(new LambdaQueryWrapper<MetricDef>()
                    .apply("LOWER(name) = {0}", normalizedName.toLowerCase()));
            if (sameName != null && !sameName.getId().equals(id)) {
                return Result.error("Metric name already exists");
            }

            existing.setName(normalizedName);
            existing.setDirection(direction);
            existing.setDescription(metricDef.getDescription());
            metricDefService.updateById(existing);
            return Result.success(metricDefService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        try {
            runMetricMapper.delete(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getMetricDefId, id));
            templateMetricDefMapper.delete(new LambdaQueryWrapper<TemplateMetricDef>().eq(TemplateMetricDef::getMetricDefId, id));
            metricDefService.removeById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
