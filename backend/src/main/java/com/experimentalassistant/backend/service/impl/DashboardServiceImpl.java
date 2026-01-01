package com.experimentalassistant.backend.service.impl;

import com.experimentalassistant.backend.dto.*;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.mapper.DashboardMapper;
import com.experimentalassistant.backend.mapper.MetricDefMapper;
import com.experimentalassistant.backend.service.DashboardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final MetricDefMapper metricDefMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper, MetricDefMapper metricDefMapper) {
        this.dashboardMapper = dashboardMapper;
        this.metricDefMapper = metricDefMapper;
    }

    @Override
    public DashboardSummaryResponse getSummary(DashboardFilter filter) {
        ensureProjectId(filter);

        DashboardStatsResult stats = dashboardMapper.getSummaryStats(filter);
        if (stats == null) {
            stats = new DashboardStatsResult();
            stats.setTotalRuns(0L);
            stats.setRunsLast7Days(0L);
            stats.setFinishedRuns(0L);
        }

        long totalRuns = stats.getTotalRuns() == null ? 0L : stats.getTotalRuns();
        long runsLast7Days = stats.getRunsLast7Days() == null ? 0L : stats.getRunsLast7Days();
        long finishedRuns = stats.getFinishedRuns() == null ? 0L : stats.getFinishedRuns();

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTotalRuns(totalRuns);
        response.setRunsLast7Days(runsLast7Days);

        if (totalRuns > 0) {
            double successRate = (double) finishedRuns / totalRuns;
            response.setSuccessRate(successRate);
        } else {
            response.setSuccessRate(0.0);
        }

        if (filter.getMetricDefId() != null) {
            ensureMetricInfo(filter);
            DashboardSummaryResponse.BestMetric bestMetric = dashboardMapper.getBestMetric(filter);
            response.setBestMetric(bestMetric);
        } else {
            response.setBestMetric(null);
        }

        return response;
    }

    @Override
    public List<DashboardTrendPoint> getTrend(DashboardFilter filter) {
        ensureProjectId(filter);
        ensureMetricInfo(filter);
        return dashboardMapper.getTrend(filter);
    }

    @Override
    public List<DashboardDistributionItem> getDistribution(DashboardFilter filter) {
        ensureProjectId(filter);
        String distributionBy = filter.getDistributionBy();
        if (distributionBy != null && distributionBy.startsWith("field:")) {
            String fieldKey = distributionBy.substring("field:".length());
            return dashboardMapper.getDistributionByField(filter, fieldKey);
        }
        return dashboardMapper.getDistribution(filter);
    }

    @Override
    public List<DashboardTopRun> getTop(DashboardFilter filter) {
        ensureProjectId(filter);
        ensureMetricInfo(filter);
        if (filter.getLimit() == null) {
            filter.setLimit(10);
        }
        return dashboardMapper.getTopRuns(filter);
    }

    private void ensureProjectId(DashboardFilter filter) {
        if (filter.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
    }

    private void ensureMetricInfo(DashboardFilter filter) {
        if (filter.getMetricDefId() == null) {
            throw new IllegalArgumentException("Metric Definition ID is required");
        }
        MetricDef metricDef = metricDefMapper.selectById(filter.getMetricDefId());
        if (metricDef == null) {
            throw new IllegalArgumentException("Invalid Metric Definition ID: " + filter.getMetricDefId());
        }
        if ("NONE".equalsIgnoreCase(metricDef.getDirection())) {
            throw new IllegalArgumentException("Metric has no direction (NONE), cannot compute best/top");
        }
        filter.setDirection(metricDef.getDirection());
    }
}
