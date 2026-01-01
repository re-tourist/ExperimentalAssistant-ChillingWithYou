package com.experimentalassistant.backend.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.entity.Project;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.mapper.ProjectMapper;
import com.experimentalassistant.backend.mapper.TemplateMapper;
import com.experimentalassistant.backend.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class ProjectTemplateBackfillRunner implements CommandLineRunner {

    private final TemplateMapper templateMapper;
    private final ProjectMapper projectMapper;
    private final ProjectService projectService;

    public ProjectTemplateBackfillRunner(TemplateMapper templateMapper,
                                         ProjectMapper projectMapper,
                                         ProjectService projectService) {
        this.templateMapper = templateMapper;
        this.projectMapper = projectMapper;
        this.projectService = projectService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        Template defaultTemplate = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getIsDefault, true));
        if (defaultTemplate == null) {
            log.info("No default template found, skip project template backfill.");
            return;
        }

        List<Project> projectsWithoutTemplate = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().isNull(Project::getTemplateId));

        if (projectsWithoutTemplate.isEmpty()) {
            return;
        }

        log.info("Backfilling template for {} projects without template.", projectsWithoutTemplate.size());

        for (Project project : projectsWithoutTemplate) {
            project.setTemplateId(defaultTemplate.getId());
            project.setProjectConfigSnapshot(null);
            projectService.updateById(project);
        }
    }
}

