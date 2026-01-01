package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.Project;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.entity.TemplateMetricDef;
import com.experimentalassistant.backend.entity.TemplateTag;
import com.experimentalassistant.backend.mapper.ProjectMapper;
import com.experimentalassistant.backend.mapper.TemplateMapper;
import com.experimentalassistant.backend.mapper.TemplateMetricDefMapper;
import com.experimentalassistant.backend.mapper.TemplateTagMapper;
import com.experimentalassistant.backend.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TemplateMapper templateMapper;
    
    @Autowired
    private TemplateMetricDefMapper templateMetricDefMapper;
    
    @Autowired
    private TemplateTagMapper templateTagMapper;
    
    @Autowired
    private com.experimentalassistant.backend.mapper.RunMapper runMapper;

    @Autowired
    private com.experimentalassistant.backend.mapper.RunMetricMapper runMetricMapper;

    @Autowired
    private com.experimentalassistant.backend.mapper.RunTagMapper runTagMapper;

    @Autowired
    private com.experimentalassistant.backend.mapper.RunNoteMapper runNoteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        // Cascade Delete Runs
        List<com.experimentalassistant.backend.entity.Run> runs = runMapper.selectList(new LambdaQueryWrapper<com.experimentalassistant.backend.entity.Run>().eq(com.experimentalassistant.backend.entity.Run::getProjectId, id));
        for (com.experimentalassistant.backend.entity.Run run : runs) {
            Long runId = run.getId();
            runMetricMapper.delete(new LambdaQueryWrapper<com.experimentalassistant.backend.entity.RunMetric>().eq(com.experimentalassistant.backend.entity.RunMetric::getRunId, runId));
            runTagMapper.delete(new LambdaQueryWrapper<com.experimentalassistant.backend.entity.RunTag>().eq(com.experimentalassistant.backend.entity.RunTag::getRunId, runId));
            runNoteMapper.delete(new LambdaQueryWrapper<com.experimentalassistant.backend.entity.RunNote>().eq(com.experimentalassistant.backend.entity.RunNote::getRunId, runId));
            runMapper.deleteById(runId);
        }
        
        return super.removeById(id);
    }
    public boolean save(Project project) {
        if (project.getTemplateId() == null) {
            Template defaultTemplate = templateMapper.selectOne(new LambdaQueryWrapper<Template>().eq(Template::getIsDefault, true));
            if (defaultTemplate != null) {
                project.setTemplateId(defaultTemplate.getId());
            }
        }
        handleTemplateSnapshot(project);
        return super.save(project);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Project project) {
        // Note: Logic for re-applying template needs care. 
        // If templateId changed, or if user explicitly requested re-sync (not handled here automatically to avoid accidental overwrite).
        // For simplicity in this patch, we update snapshot if templateId is set and snapshot is null/empty, or if it changes.
        // Ideally, frontend should clear snapshot if they want to re-fetch, or we just trust the input.
        // Let's check if templateId changed. But we don't have old entity here easily without fetch.
        // We will assume if templateId is present, we try to snapshot if snapshot is missing.
        
        if (project.getTemplateId() != null && project.getProjectConfigSnapshot() == null) {
            handleTemplateSnapshot(project);
        }
        return super.updateById(project);
    }

    private void handleTemplateSnapshot(Project project) {
        if (project.getTemplateId() != null) {
            Template template = templateMapper.selectById(project.getTemplateId());
            if (template != null) {
                // Snapshot the WHOLE template config + metadata
                try {
                    Map<String, Object> snapshot = new HashMap<>();
                    snapshot.put("sourceTemplateId", template.getId());
                    snapshot.put("sourceTemplateName", template.getName());
                    snapshot.put("domain", template.getDomain());
                    snapshot.put("configJson", template.getConfigJson()); // Inherit the flexible config
                    
                    // Snapshot relations
                    List<TemplateMetricDef> metricDefs = templateMetricDefMapper.selectList(new LambdaQueryWrapper<TemplateMetricDef>().eq(TemplateMetricDef::getTemplateId, template.getId()));
                    snapshot.put("metricDefs", metricDefs);

                    List<TemplateTag> tags = templateTagMapper.selectList(new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, template.getId()));
                    snapshot.put("tags", tags);
                    
                    project.setProjectConfigSnapshot(objectMapper.writeValueAsString(snapshot));
                } catch (Exception e) {
                    log.error("Failed to snapshot template config", e);
                    // Don't block creation, but log error
                }
            }
        }
    }
}
