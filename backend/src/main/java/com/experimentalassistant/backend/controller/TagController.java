package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.Tag;
import com.experimentalassistant.backend.service.TagService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
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
            return Result.success(existing);
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
}
