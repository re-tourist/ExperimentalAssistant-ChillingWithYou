package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.dto.TemplateDetailResponse;
import com.experimentalassistant.backend.dto.TemplateUpsertRequest;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.entity.Tag;
import com.experimentalassistant.backend.entity.Template;
import com.experimentalassistant.backend.entity.TemplateMetricDef;
import com.experimentalassistant.backend.entity.TemplateTag;
import com.experimentalassistant.backend.mapper.MetricDefMapper;
import com.experimentalassistant.backend.mapper.TagMapper;
import com.experimentalassistant.backend.mapper.TemplateMapper;
import com.experimentalassistant.backend.mapper.TemplateMetricDefMapper;
import com.experimentalassistant.backend.mapper.TemplateTagMapper;
import com.experimentalassistant.backend.service.TemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.experimentalassistant.backend.entity.TemplateField;
import com.experimentalassistant.backend.mapper.TemplateFieldMapper;

@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    private final TemplateMetricDefMapper templateMetricDefMapper;
    private final TemplateTagMapper templateTagMapper;
    private final MetricDefMapper metricDefMapper;
    private final TagMapper tagMapper;
    private final TemplateFieldMapper templateFieldMapper;

    public TemplateServiceImpl(TemplateMetricDefMapper templateMetricDefMapper,
                               TemplateTagMapper templateTagMapper,
                               MetricDefMapper metricDefMapper,
                               TagMapper tagMapper,
                               TemplateFieldMapper templateFieldMapper) {
        this.templateMetricDefMapper = templateMetricDefMapper;
        this.templateTagMapper = templateTagMapper;
        this.metricDefMapper = metricDefMapper;
        this.tagMapper = tagMapper;
        this.templateFieldMapper = templateFieldMapper;
    }

    @Override
    public Page<Template> listTemplates(int page, int size, String q, String domain) {
        Page<Template> templatePage = new Page<>(page, size);
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q)) {
            wrapper.like(Template::getName, q).or().like(Template::getDescription, q);
        }
        if (StringUtils.hasText(domain)) {
            wrapper.eq(Template::getDomain, domain);
        }
        // Ensure default templates are visible or prioritized if needed, but for now simple list
        wrapper.orderByDesc(Template::getId);
        return this.page(templatePage, wrapper);
    }

    @Override
    public TemplateDetailResponse getTemplateDetail(Long id) {
        Template template = this.getById(id);
        if (template == null) {
            return null;
        }

        TemplateDetailResponse response = new TemplateDetailResponse();
        BeanUtils.copyProperties(template, response);

        // Fetch Fields
        List<TemplateField> fields = templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getTemplateId, id)
                        .orderByAsc(TemplateField::getSortOrder)
        );
        List<TemplateDetailResponse.Field> fieldDetails = fields.stream().map(f -> {
            TemplateDetailResponse.Field fd = new TemplateDetailResponse.Field();
            BeanUtils.copyProperties(f, fd);
            return fd;
        }).collect(Collectors.toList());
        response.setFields(fieldDetails);

        // Fetch Metrics
        List<TemplateMetricDef> tMetrics = templateMetricDefMapper.selectList(
                new LambdaQueryWrapper<TemplateMetricDef>().eq(TemplateMetricDef::getTemplateId, id).orderByAsc(TemplateMetricDef::getSortOrder)
        );
        if (!tMetrics.isEmpty()) {
            List<Long> metricDefIds = tMetrics.stream().map(TemplateMetricDef::getMetricDefId).collect(Collectors.toList());
            Map<Long, MetricDef> metricDefMap = metricDefMapper.selectBatchIds(metricDefIds)
                    .stream().collect(Collectors.toMap(MetricDef::getId, m -> m));

            List<TemplateDetailResponse.TemplateMetricDetail> metricDetails = tMetrics.stream().map(tm -> {
                TemplateDetailResponse.TemplateMetricDetail detail = new TemplateDetailResponse.TemplateMetricDetail();
                detail.setMetricDefId(tm.getMetricDefId());
                detail.setIsDefault(tm.getIsDefault());
                detail.setSortOrder(tm.getSortOrder());
                MetricDef def = metricDefMap.get(tm.getMetricDefId());
                if (def != null) {
                    detail.setName(def.getName());
                    detail.setDisplayName(def.getName()); // As per previous logic
                    detail.setDirection(def.getDirection());
                }
                return detail;
            }).collect(Collectors.toList());
            response.setMetricDefs(metricDetails);
        } else {
            response.setMetricDefs(new ArrayList<>());
        }

        // Fetch Tags
        List<TemplateTag> tTags = templateTagMapper.selectList(
                new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, id).orderByAsc(TemplateTag::getSortOrder)
        );
        if (!tTags.isEmpty()) {
            List<Long> tagIds = tTags.stream().map(TemplateTag::getTagId).collect(Collectors.toList());
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds)
                    .stream().collect(Collectors.toMap(Tag::getId, t -> t));

            List<TemplateDetailResponse.TemplateTagDetail> tagDetails = tTags.stream().map(tt -> {
                TemplateDetailResponse.TemplateTagDetail detail = new TemplateDetailResponse.TemplateTagDetail();
                detail.setTagId(tt.getTagId());
                detail.setIsDefault(tt.getIsDefault());
                detail.setSortOrder(tt.getSortOrder());
                Tag tag = tagMap.get(tt.getTagId());
                if (tag != null) {
                    detail.setName(tag.getName());
                }
                return detail;
            }).collect(Collectors.toList());
            response.setTags(tagDetails);
        } else {
            response.setTags(new ArrayList<>());
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailResponse createTemplate(TemplateUpsertRequest request) {
        validateRequest(request);

        Template template = new Template();
        BeanUtils.copyProperties(request, template);
        if (!StringUtils.hasText(template.getDomain())) {
            template.setDomain("general");
        }
        template.setIsDefault(false); // Default templates are created via init script or special admin API
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        this.save(template);

        saveRelations(template.getId(), request);

        return getTemplateDetail(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailResponse updateTemplate(Long id, TemplateUpsertRequest request) {
        validateRequest(request);

        Template template = this.getById(id);
        if (template == null) {
            throw new RuntimeException("Template not found");
        }

        // Preserve isDefault and createdAt
        Boolean isDefault = template.getIsDefault();
        LocalDateTime createdAt = template.getCreatedAt();

        BeanUtils.copyProperties(request, template);
        template.setId(id);
        template.setIsDefault(isDefault);
        template.setCreatedAt(createdAt);
        
        if (!StringUtils.hasText(template.getDomain())) {
            template.setDomain("general");
        }
        template.setUpdatedAt(LocalDateTime.now());
        this.updateById(template);

        // Delete old relations
        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getTemplateId, id));
        templateMetricDefMapper.delete(new LambdaQueryWrapper<TemplateMetricDef>().eq(TemplateMetricDef::getTemplateId, id));
        templateTagMapper.delete(new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, id));

        saveRelations(id, request);

        return getTemplateDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        Template t = this.getById(id);
        if (t != null && Boolean.TRUE.equals(t.getIsDefault())) {
            throw new RuntimeException("Cannot delete default template");
        }
        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getTemplateId, id));
        templateMetricDefMapper.delete(new LambdaQueryWrapper<TemplateMetricDef>().eq(TemplateMetricDef::getTemplateId, id));
        templateTagMapper.delete(new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, id));
        this.removeById(id);
    }

    private void validateRequest(TemplateUpsertRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("Template name is required");
        }
        // Unique name check could be here or rely on DB unique constraint
    }

    private void saveRelations(Long templateId, TemplateUpsertRequest request) {
        // Save Fields
        if (!CollectionUtils.isEmpty(request.getFields())) {
            int order = 0;
            for (TemplateUpsertRequest.Field f : request.getFields()) {
                TemplateField tf = new TemplateField();
                BeanUtils.copyProperties(f, tf);
                tf.setTemplateId(templateId);
                if (tf.getSortOrder() == null) {
                    tf.setSortOrder(order++);
                }
                templateFieldMapper.insert(tf);
            }
        }

        if (!CollectionUtils.isEmpty(request.getMetricDefs())) {
            int order = 0;
            for (TemplateUpsertRequest.TemplateMetric tm : request.getMetricDefs()) {
                TemplateMetricDef entity = new TemplateMetricDef();
                entity.setTemplateId(templateId);
                entity.setMetricDefId(tm.getMetricDefId());
                entity.setIsDefault(tm.getIsDefault());
                entity.setSortOrder(tm.getSortOrder() != null ? tm.getSortOrder() : order++);
                templateMetricDefMapper.insert(entity);
            }
        }

        if (!CollectionUtils.isEmpty(request.getTags())) {
            int order = 0;
            for (TemplateUpsertRequest.TemplateTagInfo tt : request.getTags()) {
                TemplateTag entity = new TemplateTag();
                entity.setTemplateId(templateId);
                entity.setTagId(tt.getTagId());
                entity.setIsDefault(tt.getIsDefault());
                entity.setSortOrder(tt.getSortOrder() != null ? tt.getSortOrder() : order++);
                templateTagMapper.insert(entity);
            }
        }
    }
}
