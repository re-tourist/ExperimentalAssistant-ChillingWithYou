package com.experimentalassistant.backend.service.ai.impl;

import com.experimentalassistant.backend.dto.AiDraftResponse;
import com.experimentalassistant.backend.service.ai.AiProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.experimentalassistant.backend.dto.AiAnalysisResponse;

@Service("mockAiProvider")
public class MockAiProvider implements AiProvider {

    @Override
    public AiDraftResponse generate(Map<String, Object> context) {
        // ... (existing code)
        AiDraftResponse response = new AiDraftResponse();
        
        // Mock generation logic based on context
        StringBuilder md = new StringBuilder();
        md.append("# Run Analysis (AI Draft)\n\n");
        md.append("Based on the provided metrics and logs, here is a preliminary analysis:\n\n");
        
        // ... (simplified for brevity in replacement, but I should keep original logic if I can)
        // Actually, let's just use SearchReplace to append the new method
        
        // RE-IMPLEMENTING generate to match existing code exactly + adding imports
        return mockGenerate(context);
    }
    
    // Helper to keep code clean
    private AiDraftResponse mockGenerate(Map<String, Object> context) {
        AiDraftResponse response = new AiDraftResponse();
        StringBuilder md = new StringBuilder();
        md.append("# Run Analysis (AI Draft)\n\n");
        md.append("Based on the provided metrics and logs, here is a preliminary analysis:\n\n");
        
        // Safe cast wrapper
        try {
             Object runObj = context.get("run");
             if (runObj instanceof com.experimentalassistant.backend.entity.Run) {
                 com.experimentalassistant.backend.entity.Run run = (com.experimentalassistant.backend.entity.Run) runObj;
                 md.append("- **Run Name**: ").append(run.getName()).append("\n");
                 md.append("- **Status**: ").append(run.getStatus()).append("\n");
             } else if (runObj instanceof Map) {
                 Map<?,?> run = (Map<?,?>) runObj;
                 md.append("- **Run Name**: ").append(run.get("name")).append("\n");
                 md.append("- **Status**: ").append(run.get("status")).append("\n");
             }
        } catch (Exception e) {}

        md.append("\n## Key Observations\n");
        md.append("- The training process shows stable convergence (Mock).\n");
        
        response.setDraftMd(md.toString());
        
        AiDraftResponse.Extracted extracted = new AiDraftResponse.Extracted();
        extracted.setKeyFindings(Arrays.asList("Stable convergence", "Potential resource constraints"));
        extracted.setNextSteps(Arrays.asList("Check LR schedule", "Increase batch size"));
        response.setExtracted(extracted);
        return response;
    }

    @Override
    public AiAnalysisResponse analyze(Map<String, Object> fullContext) {
        String prompt = (String) fullContext.get("prompt");
        
        StringBuilder md = new StringBuilder();
        md.append("## 一、结论摘要（Summary）\n");
        md.append("- 这是一个 Mock AI 生成的分析报告。\n");
        md.append("- 你的意图是：").append(fullContext.getOrDefault("userIntent", "未指定")).append("\n");
        md.append("- 分析了 ").append(countRuns(fullContext)).append(" 个 Runs。\n\n");
        
        md.append("## 二、关键发现（Key Findings）\n");
        md.append("- 发现数据中存在随机 Mock 模式。\n");
        md.append("- Run #101 表现优于 Run #102（纯属虚构）。\n\n");
        
        md.append("## 三、可能原因分析（Hypotheses）\n");
        md.append("- 推测是随机种子导致的差异。\n\n");
        
        md.append("## 四、风险与不确定性（Risks）\n");
        md.append("- 数据量不足。\n\n");
        
        md.append("## 五、下一步实验建议（Next Actions）\n");
        md.append("1. 增加更多 Mock 数据。\n");
        md.append("2. 连接真实 LLM Provider。\n");
        
        return new AiAnalysisResponse(md.toString());
    }
    
    private int countRuns(Map<String, Object> ctx) {
        try {
            Map<String, Object> json = (Map<String, Object>) ctx.get("contextJson");
            if (json != null && json.containsKey("runs")) {
                List<?> runs = (List<?>) json.get("runs");
                return runs.size();
            }
        } catch (Exception e) {}
        return 0;
    }
}
