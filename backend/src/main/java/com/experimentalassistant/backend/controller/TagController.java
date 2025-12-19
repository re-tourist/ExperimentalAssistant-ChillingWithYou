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
        tagService.save(tag);
        return Result.success(tag);
    }
}
