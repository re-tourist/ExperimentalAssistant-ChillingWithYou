package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.Tag;
import com.experimentalassistant.backend.entity.RunTag;
import com.experimentalassistant.backend.entity.TemplateTag;
import com.experimentalassistant.backend.mapper.RunTagMapper;
import com.experimentalassistant.backend.mapper.TemplateTagMapper;
import com.experimentalassistant.backend.service.TagService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;
    private final RunTagMapper runTagMapper;
    private final TemplateTagMapper templateTagMapper;

    public TagController(TagService tagService, RunTagMapper runTagMapper, TemplateTagMapper templateTagMapper) {
        this.tagService = tagService;
        this.runTagMapper = runTagMapper;
        this.templateTagMapper = templateTagMapper;
    }

    @GetMapping
    public Result<List<Tag>> list(@RequestParam(required = false) String q) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q)) {
            wrapper.like(Tag::getName, q);
        }
        return Result.success(tagService.list(wrapper));
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        String name = tag == null ? null : tag.getName();
        if (!StringUtils.hasText(name)) {
            return Result.error("Tag name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 32) {
            return Result.error("Tag name length must be <= 32");
        }

        Tag existing = tagService.getOne(new LambdaQueryWrapper<Tag>()
                .apply("LOWER(name) = {0}", normalized.toLowerCase()));
        if (existing != null) {
            return Result.error("Tag name already exists");
        }

        Tag toSave = new Tag();
        toSave.setName(normalized);
        try {
            tagService.save(toSave);
            return Result.success(toSave);
        } catch (Exception e) {
            Tag after = tagService.getOne(new LambdaQueryWrapper<Tag>()
                    .apply("LOWER(name) = {0}", normalized.toLowerCase()));
            if (after != null) {
                return Result.success(after);
            }
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        try {
            Tag existingTag = tagService.getById(id);
            if (existingTag == null) {
                return Result.error("Tag not found");
            }

            String name = tag == null ? null : tag.getName();
            if (!StringUtils.hasText(name)) {
                return Result.error("Tag name is required");
            }
            String normalized = name.trim();
            if (normalized.length() > 32) {
                return Result.error("Tag name length must be <= 32");
            }

            Tag sameName = tagService.getOne(new LambdaQueryWrapper<Tag>()
                    .apply("LOWER(name) = {0}", normalized.toLowerCase()));
            if (sameName != null && !sameName.getId().equals(id)) {
                return Result.error("Tag name already exists");
            }

            existingTag.setName(normalized);
            tagService.updateById(existingTag);
            return Result.success(tagService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        try {
            runTagMapper.delete(new LambdaQueryWrapper<RunTag>().eq(RunTag::getTagId, id));
            templateTagMapper.delete(new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTagId, id));
            tagService.removeById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
