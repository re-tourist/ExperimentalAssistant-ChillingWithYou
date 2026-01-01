package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.RunCreateUpdateRequest;
import com.experimentalassistant.backend.dto.RunDetailResponse;
import com.experimentalassistant.backend.entity.*;
import com.experimentalassistant.backend.mapper.*;
import com.experimentalassistant.backend.service.RunService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RunServiceImpl extends ServiceImpl<RunMapper, Run> implements RunService {

    private final RunMetricMapper runMetricMapper;
    private final RunTagMapper runTagMapper;
    private final MetricDefMapper metricDefMapper;
    private final TagMapper tagMapper;

    public RunServiceImpl(RunMetricMapper runMetricMapper, RunTagMapper runTagMapper, MetricDefMapper metricDefMapper, TagMapper tagMapper) {
        this.runMetricMapper = runMetricMapper;
        this.runTagMapper = runTagMapper;
        this.metricDefMapper = metricDefMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunDetailResponse createRun(RunCreateUpdateRequest request) {
        validateRequest(request);

        Run run = new Run();
        BeanUtils.copyProperties(request, run);
        if (run.getStartTime() == null) {
            run.setStartTime(LocalDateTime.now());
        }
        if (run.getEndTime() == null) {
            run.setEndTime(run.getStartTime());
        }
        this.save(run);

        saveMetrics(run.getId(), request.getMetrics());
        saveTags(run.getId(), request.getTagIds());

        return getRunDetail(run.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunDetailResponse updateRun(Long id, RunCreateUpdateRequest request) {
        validateRequest(request);

        Run run = this.getById(id);
        if (run == null) {
            throw new RuntimeException("Run not found");
        }

        LocalDateTime existingStartTime = run.getStartTime();
        LocalDateTime existingEndTime = run.getEndTime();
        BeanUtils.copyProperties(request, run);
        if (run.getStartTime() == null) {
            run.setStartTime(existingStartTime != null ? existingStartTime : LocalDateTime.now());
        }
        if (run.getEndTime() == null) {
            run.setEndTime(existingEndTime != null ? existingEndTime : run.getStartTime());
        }
        run.setId(id);
        this.updateById(run);

        // Delete old relations
        runMetricMapper.delete(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, id));
        runTagMapper.delete(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, id));

        // Insert new relations
        saveMetrics(id, request.getMetrics());
        saveTags(id, request.getTagIds());

        return getRunDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRun(Long id) {
        runMetricMapper.delete(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, id));
        runTagMapper.delete(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, id));
        this.removeById(id);
    }

    @Override
    public RunDetailResponse getRunDetail(Long id) {
        Run run = this.getById(id);
        if (run == null) {
            return null;
        }

        RunDetailResponse response = new RunDetailResponse();
        BeanUtils.copyProperties(run, response);

        // Fetch Metrics
        List<RunMetric> runMetrics = runMetricMapper.selectList(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, id));
        if (!runMetrics.isEmpty()) {
            Set<Long> metricDefIds = runMetrics.stream().map(RunMetric::getMetricDefId).collect(Collectors.toSet());
            List<MetricDef> metricDefs = metricDefMapper.selectBatchIds(metricDefIds);
            Map<Long, MetricDef> metricDefMap = metricDefs.stream().collect(Collectors.toMap(MetricDef::getId, m -> m));

            List<RunDetailResponse.MetricDetail> metricDetails = runMetrics.stream().map(rm -> {
                MetricDef def = metricDefMap.get(rm.getMetricDefId());
                RunDetailResponse.MetricDetail detail = new RunDetailResponse.MetricDetail();
                detail.setMetricDefId(rm.getMetricDefId());
                detail.setValue(rm.getValue());
                if (def != null) {
                    detail.setName(def.getName());
                    detail.setDisplayName(def.getName());
                    detail.setDirection(def.getDirection());
                }
                return detail;
            }).collect(Collectors.toList());
            response.setMetrics(metricDetails);
        } else {
            response.setMetrics(new ArrayList<>());
        }

        // Fetch Tags
        List<RunTag> runTags = runTagMapper.selectList(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, id));
        if (!runTags.isEmpty()) {
            Set<Long> tagIds = runTags.stream().map(RunTag::getTagId).collect(Collectors.toSet());
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            response.setTags(tags);
        } else {
            response.setTags(new ArrayList<>());
        }

        return response;
    }

    @Override
    public Page<Run> listRuns(int page, int size, Long projectId, String status, String q, LocalDate dateFrom, LocalDate dateTo, List<Long> tagIds) {
        Page<Run> runPage = new Page<>(page, size);
        LambdaQueryWrapper<Run> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(Run::getProjectId, projectId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Run::getStatus, status);
        }
        if (StringUtils.hasText(q)) {
            wrapper.and(w -> w.like(Run::getName, q).or().like(Run::getModelName, q).or().like(Run::getNote, q));
        }
        if (dateFrom != null) {
            wrapper.ge(Run::getEndTime, dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            wrapper.le(Run::getEndTime, dateTo.plusDays(1).atStartOfDay());
        }

        // Tag filtering logic
        if (!CollectionUtils.isEmpty(tagIds)) {
            // Find runs that have ANY of the tags (or ALL? usually filtering implies containing the tag)
            // If multiple tags are selected, usually it's OR or AND. Let's assume OR for now, or check requirement.
            // Requirement says "Distribution by... tag". Filter "tagIds". Usually means "has one of these tags".
            // A subquery is easiest here.
            // select run_id from run_tag where tag_id in (...)
            List<RunTag> matchingRunTags = runTagMapper.selectList(new LambdaQueryWrapper<RunTag>().in(RunTag::getTagId, tagIds));
            if (matchingRunTags.isEmpty()) {
                return new Page<>(page, size); // Return empty if filtering by tags and no runs found
            }
            Set<Long> runIds = matchingRunTags.stream().map(RunTag::getRunId).collect(Collectors.toSet());
            wrapper.in(Run::getId, runIds);
        }

        wrapper.orderByDesc(Run::getId);
        return this.page(runPage, wrapper);
    }

    private void validateRequest(RunCreateUpdateRequest request) {
        if (!CollectionUtils.isEmpty(request.getMetrics())) {
            Set<Long> metricDefIds = new HashSet<>();
            for (RunCreateUpdateRequest.Metric metric : request.getMetrics()) {
                if (!metricDefIds.add(metric.getMetricDefId())) {
                    throw new RuntimeException("Duplicate metric definition ID: " + metric.getMetricDefId());
                }
            }
        }
        if (!CollectionUtils.isEmpty(request.getTagIds())) {
            Set<Long> tagIds = new HashSet<>(request.getTagIds());
            if (tagIds.size() != request.getTagIds().size()) {
                // Actually this is just a warning, we can deduplicate silently or throw.
                // The DB unique constraint is (run_id, tag_id).
                // Let's deduplicate silently by using the set in saveTags.
            }
        }
    }

    private void saveMetrics(Long runId, List<RunCreateUpdateRequest.Metric> metrics) {
        if (CollectionUtils.isEmpty(metrics)) return;
        List<RunMetric> runMetrics = metrics.stream().map(m -> {
            RunMetric rm = new RunMetric();
            rm.setRunId(runId);
            rm.setMetricDefId(m.getMetricDefId());
            rm.setValue(m.getValue());
            return rm;
        }).collect(Collectors.toList());
        // Batch insert could be optimized but loop is fine for MVP
        for (RunMetric rm : runMetrics) {
            runMetricMapper.insert(rm);
        }
    }

    private void saveTags(Long runId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) return;
        Set<Long> uniqueTagIds = new HashSet<>(tagIds);
        for (Long tagId : uniqueTagIds) {
            RunTag rt = new RunTag();
            rt.setRunId(runId);
            rt.setTagId(tagId);
            runTagMapper.insert(rt);
        }
    }
}
