package com.recruit.platform.dashboard;

public record DashboardOverviewResponse(
        long newCandidatesToday,
        long pendingFeedbackCount,
        long timeoutCount,
        long hiredCount
) {
}
