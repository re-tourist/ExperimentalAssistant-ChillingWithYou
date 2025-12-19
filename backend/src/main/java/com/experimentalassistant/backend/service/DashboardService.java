package com.experimentalassistant.backend.service;

import com.experimentalassistant.backend.dto.*;
import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary(DashboardFilter filter);
    List<DashboardTrendPoint> getTrend(DashboardFilter filter);
    List<DashboardDistributionItem> getDistribution(DashboardFilter filter);
    List<DashboardTopRun> getTop(DashboardFilter filter);
}
