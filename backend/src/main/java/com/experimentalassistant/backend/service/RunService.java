package com.experimentalassistant.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.experimentalassistant.backend.dto.RunCreateUpdateRequest;
import com.experimentalassistant.backend.dto.RunDetailResponse;
import com.experimentalassistant.backend.entity.Run;

import java.time.LocalDate;
import java.util.List;

public interface RunService extends IService<Run> {
    
    RunDetailResponse createRun(RunCreateUpdateRequest request);
    
    RunDetailResponse updateRun(Long id, RunCreateUpdateRequest request);
    
    RunDetailResponse getRunDetail(Long id);
    
    Page<Run> listRuns(int page, int size, Long projectId, String status, String q, LocalDate dateFrom, LocalDate dateTo, List<Long> tagIds);

    void deleteRun(Long id);
}
