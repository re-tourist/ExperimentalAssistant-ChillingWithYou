package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.experimentalassistant.backend.common.PageResult;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.Project;
import com.experimentalassistant.backend.service.ProjectService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Result<PageResult<Project>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String q) {
        Page<Project> projectPage = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q)) {
            wrapper.like(Project::getName, q).or().like(Project::getDescription, q);
        }
        projectService.page(projectPage, wrapper);
        
        PageResult<Project> pageResult = new PageResult<>(
                projectPage.getRecords(),
                projectPage.getTotal(),
                projectPage.getCurrent(),
                projectPage.getSize()
        );
        return Result.success(pageResult);
    }

    @PostMapping
    public Result<Project> create(@RequestBody Project project) {
        projectService.save(project);
        return Result.success(project);
    }

    @PutMapping("/{id}")
    public Result<Project> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateById(project);
        return Result.success(project);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return Result.success();
    }
}
