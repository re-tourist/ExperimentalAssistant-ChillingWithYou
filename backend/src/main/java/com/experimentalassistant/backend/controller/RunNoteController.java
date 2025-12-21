package com.experimentalassistant.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.RunConclusionResponse;
import com.experimentalassistant.backend.dto.RunNoteCreateRequest;
import com.experimentalassistant.backend.dto.RunNoteResponse;
import com.experimentalassistant.backend.dto.RunNoteUpdateRequest;
import com.experimentalassistant.backend.entity.RunNote;
import com.experimentalassistant.backend.service.RunNoteService;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RunNoteController {

    @Autowired
    private RunNoteService runNoteService;

    // --- Notes CRUD ---

    @GetMapping("/runs/{runId}/notes")
    public Result<List<RunNoteResponse>> getNotes(@PathVariable Long runId, @RequestParam(required = false) String type) {
        LambdaQueryWrapper<RunNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunNote::getRunId, runId);
        if (StringUtils.hasText(type)) {
            wrapper.eq(RunNote::getType, type);
        } else {
            // By default, maybe exclude CONCLUSION if strictly asking for notes?
            // Requirement says: get notes?type=NOTE|AI_DRAFT
            // If no type specified, return all or just NOTE? Let's return all for now or follow common sense.
            // Usually notes list might exclude conclusion if conclusion is special.
            // But let's just return what matches.
        }
        wrapper.orderByDesc(RunNote::getCreatedAt);
        
        List<RunNote> list = runNoteService.list(wrapper);
        List<RunNoteResponse> responses = list.stream().map(this::toResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @PostMapping("/runs/{runId}/notes")
    public Result<RunNoteResponse> createNote(@PathVariable Long runId, @RequestBody RunNoteCreateRequest request) {
        if (!StringUtils.hasText(request.getContentMd())) {
            return Result.error("Content is required");
        }
        RunNote note = new RunNote();
        note.setRunId(runId);
        note.setType(StringUtils.hasText(request.getType()) ? request.getType() : "NOTE");
        note.setTitle(request.getTitle());
        note.setContentMd(request.getContentMd());
        
        runNoteService.save(note);
        return Result.success(toResponse(note));
    }

    @PutMapping("/runs/{runId}/notes/{noteId}")
    public Result<RunNoteResponse> updateNote(@PathVariable Long runId, @PathVariable Long noteId, @RequestBody RunNoteUpdateRequest request) {
        RunNote note = runNoteService.getById(noteId);
        if (note == null || !note.getRunId().equals(runId)) {
            return Result.error("Note not found");
        }
        
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContentMd() != null) {
            note.setContentMd(request.getContentMd());
        }
        
        runNoteService.updateById(note);
        return Result.success(toResponse(note));
    }

    @DeleteMapping("/runs/{runId}/notes/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long runId, @PathVariable Long noteId) {
        RunNote note = runNoteService.getById(noteId);
        if (note == null || !note.getRunId().equals(runId)) {
            return Result.error("Note not found");
        }
        runNoteService.removeById(noteId);
        return Result.success();
    }

    // --- Conclusion ---

    @GetMapping("/runs/{runId}/conclusion")
    public Result<RunConclusionResponse> getConclusion(@PathVariable Long runId) {
        LambdaQueryWrapper<RunNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunNote::getRunId, runId);
        wrapper.eq(RunNote::getType, "CONCLUSION");
        wrapper.last("LIMIT 1"); // Should be only one
        
        RunNote note = runNoteService.getOne(wrapper);
        RunConclusionResponse response = new RunConclusionResponse();
        response.setRunId(runId);
        response.setConclusion(note != null ? toResponse(note) : null);
        
        return Result.success(response);
    }

    @PutMapping("/runs/{runId}/conclusion")
    public Result<RunNoteResponse> upsertConclusion(@PathVariable Long runId, @RequestBody RunNoteUpdateRequest request) {
        if (!StringUtils.hasText(request.getContentMd())) {
            return Result.error("Content is required");
        }

        LambdaQueryWrapper<RunNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RunNote::getRunId, runId);
        wrapper.eq(RunNote::getType, "CONCLUSION");
        wrapper.last("LIMIT 1");
        
        RunNote note = runNoteService.getOne(wrapper);
        if (note == null) {
            note = new RunNote();
            note.setRunId(runId);
            note.setType("CONCLUSION");
        }
        
        note.setTitle(request.getTitle() != null ? request.getTitle() : "Conclusion");
        note.setContentMd(request.getContentMd());
        
        runNoteService.saveOrUpdate(note);
        return Result.success(toResponse(note));
    }
    
    // --- Markdown Refs ---
    
    @PostMapping("/markdown/refs")
    public Result<List<Long>> parseRefs(@RequestBody RefRequest request) {
        if (!StringUtils.hasText(request.getTextMd())) {
            return Result.success(Collections.emptyList());
        }
        // Regex for [[run:123]]
        Pattern pattern = Pattern.compile("\\[\\[run:(\\d+)\\]\\]");
        Matcher matcher = pattern.matcher(request.getTextMd());
        
        List<Long> ids = matcher.results()
            .map(m -> Long.parseLong(m.group(1)))
            .distinct()
            .collect(Collectors.toList());
            
        return Result.success(ids);
    }
    
    @Data
    public static class RefRequest {
        private String textMd;
    }

    private RunNoteResponse toResponse(RunNote note) {
        RunNoteResponse response = new RunNoteResponse();
        BeanUtils.copyProperties(note, response);
        return response;
    }
}
