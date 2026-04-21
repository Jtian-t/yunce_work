package com.recruit.platform.dashboard;

public record DepartmentEfficiencyResponse(
        String department,
        double averageDays,
        long completedCount
) {
}
