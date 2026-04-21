package com.recruit.platform.dashboard;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    DashboardOverviewResponse overview() {
        return dashboardService.overview();
    }

    @GetMapping("/funnel")
    List<DashboardMetricResponse> funnel() {
        return dashboardService.funnel();
    }

    @GetMapping("/status-distribution")
    List<DashboardMetricResponse> statusDistribution() {
        return dashboardService.statusDistribution();
    }

    @GetMapping("/department-efficiency")
    List<DepartmentEfficiencyResponse> departmentEfficiency() {
        return dashboardService.departmentEfficiency();
    }

    @GetMapping("/alerts")
    List<AlertResponse> alerts() {
        return dashboardService.alerts();
    }
}
