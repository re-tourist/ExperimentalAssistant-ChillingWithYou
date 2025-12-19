package com.experimentalassistant.backend.controller;

import com.experimentalassistant.backend.common.Result;
import com.experimentalassistant.backend.dto.*;
import com.experimentalassistant.backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<DashboardSummaryResponse> summary(DashboardFilter filter) {
        try {
            return Result.success(dashboardService.getSummary(filter));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/trend")
    public Result<List<DashboardTrendPoint>> trend(DashboardFilter filter) {
        try {
            return Result.success(dashboardService.getTrend(filter));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/distribution")
    public Result<List<DashboardDistributionItem>> distribution(DashboardFilter filter, @RequestParam(name = "by") String by) {
        try {
            filter.setDistributionBy(by);
            return Result.success(dashboardService.getDistribution(filter));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/top")
    public Result<List<DashboardTopRun>> top(DashboardFilter filter) {
        try {
            return Result.success(dashboardService.getTop(filter));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
