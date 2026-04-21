package com.recruit.platform.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

record DailyReportSummary(
        long newCandidates,
        long feedbackCompleted,
        long pendingReview,
        long interviews,
        long offers,
        long timeout
) {
}

record DailyReportProgressItem(
        Long candidateId,
        String candidateName,
        String position,
        String department,
        String statusLabel,
        OffsetDateTime eventTime,
        String note
) {
}

record DailyReportResponse(
        LocalDate reportDate,
        DailyReportSummary summary,
        List<DailyReportProgressItem> progress,
        List<ReportMetricResponse> statusBreakdown,
        List<ReportDepartmentStatResponse> departmentStats,
        List<ReportAlertResponse> alerts
) {
}

record ReportMetricResponse(
        String label,
        long value
) {
}

record ReportDepartmentStatResponse(
        String department,
        double averageDays,
        long completedCount
) {
}

record ReportAlertResponse(
        Long assignmentId,
        Long candidateId,
        String candidateName,
        String position,
        String department,
        long overdueHours
) {
}
