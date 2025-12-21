package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.AiDraftRequest;
import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.entity.*;
import com.experimentalassistant.backend.mapper.*;
import com.experimentalassistant.backend.service.AiAnalysisService;
import com.experimentalassistant.backend.service.RunNoteService;
import com.experimentalassistant.backend.service.RunService;
import com.experimentalassistant.backend.service.ai.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final AiProvider mockAiProvider;
    private final AiProvider httpAiProvider;
    private final AiProperties aiProperties;

    @Autowired
    private RunService runService;

    @Autowired
    private RunNoteService runNoteService;

    @Autowired
    private RunMetricMapper runMetricMapper;

    @Autowired
    private RunTagMapper runTagMapper;
    
    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private MetricDefMapper metricDefMapper;

    @Autowired
    public AiAnalysisServiceImpl(
            @Qualifier("mockAiProvider") AiProvider mockAiProvider,
            @Qualifier("httpAiProvider") AiProvider httpAiProvider,
            AiProperties aiProperties) {
        this.mockAiProvider = mockAiProvider;
        this.httpAiProvider = httpAiProvider;
        this.aiProperties = aiProperties;
    }

    @Override
    @Transactional
    public AiDraftResponse generateDraft(AiDraftRequest request) {
        Long runId = request.getRunId();
        Run run = runService.getById(runId);
        if (run == null) {
            throw new RuntimeException("Run not found");
        }

        Map<String, Object> context = new HashMap<>();
        context.put("run", run);
        context.put("userHint", request.getUserHint());

        if (request.isIncludeMetrics()) {
            List<RunMetric> runMetrics = runMetricMapper.selectList(new LambdaQueryWrapper<RunMetric>().eq(RunMetric::getRunId, runId));
            List<Map<String, Object>> metricsData = runMetrics.stream().map(rm -> {
                Map<String, Object> m = new HashMap<>();
                m.put("value", rm.getValue());
                MetricDef def = metricDefMapper.selectById(rm.getMetricDefId());
                if (def != null) {
                    m.put("name", def.getName());
                    m.put("direction", def.getDirection());
                }
                return m;
            }).collect(Collectors.toList());
            context.put("metrics", metricsData);
        }

        if (request.isIncludeTags()) {
            List<RunTag> runTags = runTagMapper.selectList(new LambdaQueryWrapper<RunTag>().eq(RunTag::getRunId, runId));
            List<String> tagNames = runTags.stream().map(rt -> {
                Tag t = tagMapper.selectById(rt.getTagId());
                return t != null ? t.getName() : String.valueOf(rt.getTagId());
            }).collect(Collectors.toList());
            context.put("tags", tagNames);
        }

        if (request.isIncludeTemplate() && run.getTemplateId() != null) {
            Template template = templateMapper.selectById(run.getTemplateId());
            if (template != null) {
                context.put("template", template);
            }
        }

        if (request.isIncludeNotes()) {
            List<RunNote> recentNotes = runNoteService.list(new LambdaQueryWrapper<RunNote>()
                    .eq(RunNote::getRunId, runId)
                    .ne(RunNote::getType, "AI_DRAFT") // Exclude drafts
                    .orderByDesc(RunNote::getCreatedAt)
                    .last("LIMIT 5"));
            List<String> noteContents = recentNotes.stream().map(RunNote::getContentMd).collect(Collectors.toList());
            context.put("notes", noteContents);
        }

        // Call Provider with Logic
        AiDraftResponse response = callAiProvider(context);

        // Optional: Save Draft
        if (request.isSaveDraft()) {
            RunNote note = new RunNote();
            note.setRunId(runId);
            note.setType("AI_DRAFT");
            note.setTitle("AI Draft");
            note.setContentMd(response.getDraftMd());
            runNoteService.save(note);
        }

        return response;
    }

    private AiDraftResponse callAiProvider(Map<String, Object> context) {
        String provider = aiProperties.getProvider();
        boolean useHttp = "http".equalsIgnoreCase(provider)
                && aiProperties.getHttp().getBaseUrl() != null
                && !aiProperties.getHttp().getBaseUrl().isEmpty();

        if (useHttp) {
            long start = System.currentTimeMillis();
            try {
                log.info("Calling HttpAiProvider: url={}", aiProperties.getHttp().getBaseUrl());
                AiDraftResponse res = httpAiProvider.generate(context);
                log.info("HttpAiProvider success, duration={}ms", System.currentTimeMillis() - start);
                return res;
            } catch (Exception e) {
                log.error("HttpAiProvider failed, duration={}ms, error={}. Falling back to Mock.",
                        System.currentTimeMillis() - start, e.getMessage());
                // Fallback
            }
        } else {
            if ("http".equalsIgnoreCase(provider)) {
                log.warn("AI provider is 'http' but config is missing. Falling back to Mock.");
            }
        }

        long start = System.currentTimeMillis();
        AiDraftResponse res = mockAiProvider.generate(context);
        log.info("MockAiProvider used, duration={}ms", System.currentTimeMillis() - start);
        return res;
    }
}
