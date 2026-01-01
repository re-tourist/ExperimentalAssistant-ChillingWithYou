package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.entity.Domain;
import com.experimentalassistant.backend.service.DomainService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public Result<List<Domain>> list(@RequestParam(required = false) String q) {
        LambdaQueryWrapper<Domain> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q)) {
            wrapper.like(Domain::getName, q);
        }
        wrapper.orderByDesc(Domain::getCreatedAt);
        return Result.success(domainService.list(wrapper));
    }

    @PostMapping
    public Result<Domain> create(@RequestBody Domain domain) {
        String name = domain == null ? null : domain.getName();
        if (!StringUtils.hasText(name)) {
            return Result.error("Domain name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 32) {
            return Result.error("Domain name length must be <= 32");
        }

        Domain existing = domainService.getOne(new LambdaQueryWrapper<Domain>()
                .apply("LOWER(name) = {0}", normalized.toLowerCase()));
        if (existing != null) {
            return Result.error("Domain name already exists");
        }

        Domain toSave = new Domain();
        toSave.setName(normalized);
        toSave.setCreatedAt(LocalDateTime.now());
        toSave.setUpdatedAt(LocalDateTime.now());
        try {
            domainService.save(toSave);
            return Result.success(toSave);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Domain> update(@PathVariable Long id, @RequestBody Domain domain) {
        try {
            Domain existing = domainService.getById(id);
            if (existing == null) {
                return Result.error("Domain not found");
            }

            String name = domain == null ? null : domain.getName();
            if (!StringUtils.hasText(name)) {
                return Result.error("Domain name is required");
            }
            String normalized = name.trim();
            if (normalized.length() > 32) {
                return Result.error("Domain name length must be <= 32");
            }

            Domain sameName = domainService.getOne(new LambdaQueryWrapper<Domain>()
                    .apply("LOWER(name) = {0}", normalized.toLowerCase()));
            if (sameName != null && !sameName.getId().equals(id)) {
                return Result.error("Domain name already exists");
            }

            existing.setName(normalized);
            existing.setUpdatedAt(LocalDateTime.now());
            domainService.updateById(existing);
            return Result.success(domainService.getById(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            domainService.removeById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
