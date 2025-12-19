package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.experimentalassistant.backend.common.PageResult;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.RunCreateUpdateRequest;
import com.experimentalassistant.backend.dto.RunDetailResponse;
import com.experimentalassistant.backend.entity.Run;
import com.experimentalassistant.backend.service.RunService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping
    public Result<PageResult<Run>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) Long projectId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                        @RequestParam(required = false) List<Long> tagIds) {
        Page<Run> runPage = runService.listRuns(page, size, projectId, status, q, dateFrom, dateTo, tagIds);
        return Result.success(new PageResult<>(
                runPage.getRecords(),
                runPage.getTotal(),
                runPage.getCurrent(),
                runPage.getSize()
        ));
    }

    @GetMapping("/{id}")
    public Result<RunDetailResponse> get(@PathVariable Long id) {
        RunDetailResponse detail = runService.getRunDetail(id);
        if (detail == null) {
            return Result.error("Run not found");
        }
        return Result.success(detail);
    }

    @PostMapping
    public Result<RunDetailResponse> create(@RequestBody RunCreateUpdateRequest request) {
        try {
            RunDetailResponse created = runService.createRun(request);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<RunDetailResponse> update(@PathVariable Long id, @RequestBody RunCreateUpdateRequest request) {
        try {
            RunDetailResponse updated = runService.updateRun(id, request);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        runService.deleteRun(id);
        return Result.success();
    }
}
