package com.experimentalassistant.backend.mapper;

import com.experimentalassistant.backend.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DashboardMapper {
    DashboardStatsResult getSummaryStats(@Param("filter") DashboardFilter filter);

    DashboardSummaryResponse.BestMetric getBestMetric(@Param("filter") DashboardFilter filter);

    List<DashboardTrendPoint> getTrend(@Param("filter") DashboardFilter filter);

    List<DashboardDistributionItem> getDistribution(@Param("filter") DashboardFilter filter);

    List<DashboardTopRun> getTopRuns(@Param("filter") DashboardFilter filter);
}
