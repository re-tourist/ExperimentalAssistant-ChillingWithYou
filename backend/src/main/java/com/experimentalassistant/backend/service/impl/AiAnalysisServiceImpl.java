package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.config.AiProperties;
import com.experimentalassistant.backend.dto.AiAnalysisRequest;
import com.experimentalassistant.backend.dto.AiAnalysisResponse;
import com.experimentalassistant.backend.dto.AiDraftRequest;
import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.entity.*;
import com.experimentalassistant.backend.mapper.*;
import com.experimentalassistant.backend.service.AiAnalysisService;
import com.experimentalassistant.backend.service.RunService;
import com.experimentalassistant.backend.service.ai.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RunService runService;

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

        // Call Provider with Logic
        AiDraftResponse response = callAiProvider(context);

        return response;
    }

    @Override
    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        // 1. Construct the prompt with 4 sections
        String prompt = constructPrompt(request);
        
        // 2. Prepare context for provider
        Map<String, Object> fullContext = new HashMap<>();
        fullContext.put("prompt", prompt);
        fullContext.put("userIntent", request.getUserIntent());
        fullContext.put("contextJson", request.getContextJson());
        
        // 3. Call Provider (reuse existing logic with generic method)
        return callAiProviderForAnalysis(fullContext);
    }

    private String constructPrompt(AiAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        
        // SECTION 1 — System Instruction
        sb.append("You are an expert experiment analysis assistant. Your goal is to help users analyze experiment data, compare runs, and provide actionable insights.\n\n");
        
        // SECTION 2 — User Intent
        sb.append("## User Intent\n");
        sb.append(request.getUserIntent() != null ? request.getUserIntent() : "Please analyze the provided context.").append("\n\n");
        
        // SECTION 3 — Selected Context
        sb.append("## Selected Context (JSON)\n");
        sb.append("```json\n");
        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request.getContextJson()));
        } catch (Exception e) {
            sb.append("{}");
        }
        sb.append("\n```\n\n");
        
        // SECTION 4 — Output Contract
        sb.append("## Output Contract\n");
        sb.append("You MUST output your response strictly in the following Markdown format:\n");
        sb.append("## 一、结论摘要（Summary）\n(Bullet points)\n\n");
        sb.append("## 二、关键发现（Key Findings）\n(Fact-based findings with evidence)\n\n");
        sb.append("## 三、可能原因分析（Hypotheses）\n(Inferences)\n\n");
        sb.append("## 四、风险与不确定性（Risks）\n\n");
        sb.append("## 五、下一步实验建议（Next Actions）\n\n");
        
        return sb.toString();
    }

    private AiAnalysisResponse callAiProviderForAnalysis(Map<String, Object> context) {
        String provider = aiProperties.getProvider();
        boolean useHttp = "http".equalsIgnoreCase(provider)
                && aiProperties.getHttp().getBaseUrl() != null
                && !aiProperties.getHttp().getBaseUrl().isEmpty();

        if (useHttp) {
            long start = System.currentTimeMillis();
            try {
                log.info("Calling HttpAiProvider (Analyze): url={}", aiProperties.getHttp().getBaseUrl());
                AiAnalysisResponse res = httpAiProvider.analyze(context);
                log.info("HttpAiProvider success, duration={}ms", System.currentTimeMillis() - start);
                return res;
            } catch (Exception e) {
                log.error("HttpAiProvider failed, duration={}ms, error={}. Falling back to Mock.",
                        System.currentTimeMillis() - start, e.getMessage());
            }
        }
        
        long start = System.currentTimeMillis();
        AiAnalysisResponse res = mockAiProvider.analyze(context);
        log.info("MockAiProvider used (Analyze), duration={}ms", System.currentTimeMillis() - start);
        return res;
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
