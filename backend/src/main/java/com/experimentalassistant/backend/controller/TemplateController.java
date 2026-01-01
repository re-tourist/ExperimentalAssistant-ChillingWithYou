package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.experimentalassistant.backend.common.PageResult;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.TemplateDetailResponse;
import com.experimentalassistant.backend.dto.TemplateUpsertRequest;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.service.TemplateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public Result<PageResult<Template>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String domain) {
        Page<Template> templatePage = templateService.listTemplates(page, size, q, domain);
        return Result.success(new PageResult<>(
                templatePage.getRecords(),
                templatePage.getTotal(),
                templatePage.getCurrent(),
                templatePage.getSize()
        ));
    }

    @GetMapping("/{id}")
    public Result<TemplateDetailResponse> get(@PathVariable Long id) {
        TemplateDetailResponse detail = templateService.getTemplateDetail(id);
        if (detail == null) {
            return Result.error("Template not found");
        }
        return Result.success(detail);
    }

    @PostMapping
    public Result<TemplateDetailResponse> create(@RequestBody TemplateUpsertRequest request) {
        try {
            return Result.success(templateService.createTemplate(request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<TemplateDetailResponse> update(@PathVariable Long id, @RequestBody TemplateUpsertRequest request) {
        try {
            return Result.success(templateService.updateTemplate(id, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.success();
    }
}
