package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

import com.experimentalassistant.backend.entity.RunFieldValue;
import com.experimentalassistant.backend.mapper.RunFieldValueMapper;

@Service
public class RunServiceImpl extends ServiceImpl<RunMapper, Run> implements RunService {

    private final RunMetricMapper runMetricMapper;
    private final RunTagMapper runTagMapper;
    private final RunNoteMapper runNoteMapper;
    private final MetricDefMapper metricDefMapper;
    private final TagMapper tagMapper;
    private final RunFieldValueMapper runFieldValueMapper;
    private final ProjectMapper projectMapper;
    private final TemplateFieldMapper templateFieldMapper;

    public RunServiceImpl(RunMetricMapper runMetricMapper,
                          RunTagMapper runTagMapper,
                          RunNoteMapper runNoteMapper,
                          MetricDefMapper metricDefMapper,
                          TagMapper tagMapper,
                          RunFieldValueMapper runFieldValueMapper,
                          ProjectMapper projectMapper,
                          TemplateFieldMapper templateFieldMapper) {
        this.runMetricMapper = runMetricMapper;
        this.runTagMapper = runTagMapper;
        this.runNoteMapper = runNoteMapper;
        this.metricDefMapper = metricDefMapper;
        this.tagMapper = tagMapper;
        this.runFieldValueMapper = runFieldValueMapper;
        this.projectMapper = projectMapper;
        this.templateFieldMapper = templateFieldMapper;
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
        
        // Sync legacy fields from fieldValues for backward compatibility
        syncLegacyFields(run, request.getFieldValues());
        
        this.save(run);

        saveMetrics(run.getId(), request.getMetrics());
        saveTags(run.getId(), request.getTagIds());
        saveFieldValues(run.getId(), request.getFieldValues());

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

        BeanUtils.copyProperties(request, run);
        
        // Sync legacy fields
        syncLegacyFields(run, request.getFieldValues());
        
        run.setId(id);
        this.updateById(run);

        // Delete old relations
        runMetricMapper.delete(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, id));
        runTagMapper.delete(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, id));
        runFieldValueMapper.delete(new LambdaQueryWrapper<RunFieldValue>().eq(RunFieldValue::getRunId, id));

        // Insert new relations
        saveMetrics(id, request.getMetrics());
        saveTags(id, request.getTagIds());
        saveFieldValues(id, request.getFieldValues());

        return getRunDetail(id);
    }
    
    private void syncLegacyFields(Run run, Map<String, Object> values) {
        if (CollectionUtils.isEmpty(values)) return;
        
        if (values.containsKey("model")) run.setModelName(String.valueOf(values.get("model")));
        if (values.containsKey("dataset")) run.setDatasetName(String.valueOf(values.get("dataset")));
        if (values.containsKey("optimizer")) run.setOptimizer(String.valueOf(values.get("optimizer")));
        
        if (values.containsKey("learning_rate")) {
            try { run.setLr(Double.parseDouble(String.valueOf(values.get("learning_rate")))); } catch (Exception e) {}
        }
        if (values.containsKey("batch_size")) {
            try { run.setBatchSize(Integer.parseInt(String.valueOf(values.get("batch_size")))); } catch (Exception e) {}
        }
        if (values.containsKey("epochs")) {
            try { run.setEpochs(Integer.parseInt(String.valueOf(values.get("epochs")))); } catch (Exception e) {}
        }
        if (values.containsKey("seed")) {
            try { run.setSeed(Integer.parseInt(String.valueOf(values.get("seed")))); } catch (Exception e) {}
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRun(Long id) {
        runMetricMapper.delete(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, id));
        runTagMapper.delete(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, id));
        runNoteMapper.delete(new LambdaQueryWrapper<RunNote>().eq(RunNote::getRunId, id));
        runFieldValueMapper.delete(new LambdaQueryWrapper<RunFieldValue>().eq(RunFieldValue::getRunId, id));
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

        // Fetch Field Values
        List<RunFieldValue> fields = runFieldValueMapper.selectList(new LambdaQueryWrapper<RunFieldValue>().eq(RunFieldValue::getRunId, id));
        if (!fields.isEmpty()) {
            Map<String, Object> fieldMap = fields.stream()
                .collect(Collectors.toMap(RunFieldValue::getFieldKey, RunFieldValue::getValueText));
            response.setFieldValues(fieldMap);
        }

        return response;
    }

    private void saveFieldValues(Long runId, Map<String, Object> fieldValues) {
        if (CollectionUtils.isEmpty(fieldValues)) return;
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            if (entry.getValue() != null) {
                RunFieldValue val = new RunFieldValue();
                val.setRunId(runId);
                val.setFieldKey(entry.getKey());
                val.setValueText(String.valueOf(entry.getValue()));
                runFieldValueMapper.insert(val);
            }
        }
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
        if (request.getProjectId() == null) {
            throw new RuntimeException("Project is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("Run name is required");
        }
        request.setName(request.getName().trim());
        if (!StringUtils.hasText(request.getStatus())) {
            request.setStatus("RUNNING");
        }
        
        // Dynamic Fields Validation
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw new RuntimeException("Project not found");
        }
        if (project.getTemplateId() != null) {
            List<TemplateField> templateFields = templateFieldMapper.selectList(
                    new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getTemplateId, project.getTemplateId())
            );
            
            Map<String, Object> inputFields = request.getFieldValues() != null ? request.getFieldValues() : Map.of();
            
            for (TemplateField field : templateFields) {
                String key = field.getFieldKey();
                Object val = inputFields.get(key);
                
                // 1. Required check
                if (Boolean.TRUE.equals(field.getIsRequired())) {
                    if (val == null || String.valueOf(val).trim().isEmpty()) {
                        throw new RuntimeException("Field '" + field.getLabel() + "' is required");
                    }
                }
                
                // 2. Type check (Basic)
                if (val != null && !String.valueOf(val).isEmpty()) {
                    String strVal = String.valueOf(val);
                    if ("NUMBER".equalsIgnoreCase(field.getFieldType())) {
                        try {
                            Double.parseDouble(strVal);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Field '" + field.getLabel() + "' must be a number");
                        }
                    }
                }
            }
        }

        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new RuntimeException("Run start time must be before or equal to end time");
            }
        }

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
