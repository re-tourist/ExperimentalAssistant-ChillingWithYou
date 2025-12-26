package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.Project;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.mapper.ProjectMapper;
import com.experimentalassistant.backend.mapper.TemplateMapper;
import com.experimentalassistant.backend.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Autowired
    private TemplateMapper templateMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Project project) {
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
                    // We can also fetch relations (metrics/tags) and put them here if needed.
                    // For Phase 1, let's snapshot what's on the Template entity + maybe the configJson is enough if it holds everything.
                    // Wait, Template entity has configJson string.
                    
                    project.setProjectConfigSnapshot(objectMapper.writeValueAsString(snapshot));
                } catch (Exception e) {
                    log.error("Failed to snapshot template config", e);
                    // Don't block creation, but log error
                }
            }
        }
    }
}
