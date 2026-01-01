package com.experimentalassistant.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.experimentalassistant.backend.dto.TemplateDetailResponse;
import com.experimentalassistant.backend.dto.TemplateUpsertRequest;
import com.experimentalassistant.backend.entity.Template;

public interface TemplateService extends IService<Template> {
    Page<Template> listTemplates(int page, int size, String q, String domain);
    TemplateDetailResponse getTemplateDetail(Long id);
    TemplateDetailResponse createTemplate(TemplateUpsertRequest request);
    TemplateDetailResponse updateTemplate(Long id, TemplateUpsertRequest request);
    void deleteTemplate(Long id);
}
