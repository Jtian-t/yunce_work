package com.recruit.platform.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.candidate.CandidateRepository;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.dashboard.DashboardService;
import com.recruit.platform.workflow.DepartmentAssignmentRepository;
import com.recruit.platform.workflow.WorkflowEventRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final CandidateRepository candidateRepository;
    private final DepartmentAssignmentRepository assignmentRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final ReportSnapshotRepository reportSnapshotRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public DailyReportResponse getDailyReport(LocalDate date) {
        return reportSnapshotRepository.findByReportDate(date)
                .map(snapshot -> read(snapshot.getPayloadJson()))
                .orElseGet(() -> generateAndPersist(date));
    }

    @Transactional
    public DailyReportResponse generateAndPersist(LocalDate date) {
        DailyReportResponse report = build(date);
        String payload = write(report);
        ReportSnapshot snapshot = reportSnapshotRepository.findByReportDate(date).orElseGet(ReportSnapshot::new);
        snapshot.setReportDate(date);
        snapshot.setPayloadJson(payload);
        reportSnapshotRepository.save(snapshot);
        return report;
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Shanghai")
    public void generateTodaySnapshot() {
        generateAndPersist(LocalDate.now(ZoneId.of("Asia/Shanghai")));
    }

    private DailyReportResponse build(LocalDate date) {
        var candidates = candidateRepository.findAll();
        long newCandidates = candidates.stream().filter(candidate -> date.equals(candidate.getSubmittedDate())).count();
        long feedbackCompleted = assignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                .count();
        long pendingReview = assignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.OPEN)
                .count();
        long interviews = candidates.stream()
                .filter(candidate -> candidate.getStatus().name().contains("INTERVIEW"))
                .count();
        long offers = candidates.stream()
                .filter(candidate -> candidate.getStatus().name().contains("OFFER"))
                .count();
        long timeout = candidates.stream().filter(candidate -> candidate.getStatus().name().equals("TIMEOUT")).count();

        List<DailyReportProgressItem> progress = workflowEventRepository.findAll().stream()
                .filter(event -> event.getOccurredAt().toLocalDate().equals(date))
                .map(event -> new DailyReportProgressItem(
                        event.getCandidate().getId(),
                        event.getCandidate().getName(),
                        event.getCandidate().getPosition(),
                        event.getCandidate().getDepartment() == null ? null : event.getCandidate().getDepartment().getName(),
                        event.getStatusLabel(),
                        event.getOccurredAt(),
                        event.getNote()
                ))
                .toList();

        return new DailyReportResponse(
                date,
                new DailyReportSummary(newCandidates, feedbackCompleted, pendingReview, interviews, offers, timeout),
                progress,
                dashboardService.statusDistribution().stream()
                        .map(metric -> new ReportMetricResponse(metric.label(), metric.value()))
                        .toList(),
                dashboardService.departmentEfficiency().stream()
                        .map(stat -> new ReportDepartmentStatResponse(stat.department(), stat.averageDays(), stat.completedCount()))
                        .toList(),
                dashboardService.alerts().stream()
                        .map(alert -> new ReportAlertResponse(
                                alert.assignmentId(),
                                alert.candidateId(),
                                alert.candidateName(),
                                alert.position(),
                                alert.department(),
                                alert.overdueHours()
                        ))
                        .toList()
        );
    }

    private String write(DailyReportResponse report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize report", exception);
        }
    }

    private DailyReportResponse read(String payload) {
        try {
            return objectMapper.readValue(payload, DailyReportResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize report", exception);
        }
    }
}
