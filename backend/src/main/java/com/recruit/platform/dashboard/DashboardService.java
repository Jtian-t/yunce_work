package com.recruit.platform.dashboard;

import com.recruit.platform.candidate.CandidateRepository;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.workflow.DepartmentAssignment;
import com.recruit.platform.workflow.DepartmentAssignmentRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CandidateRepository candidateRepository;
    private final DepartmentAssignmentRepository departmentAssignmentRepository;
    private final CurrentUserService currentUserService;

    public DashboardOverviewResponse overview() {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        List<com.recruit.platform.candidate.Candidate> candidates = candidateRepository.findAll();
        LocalDate today = LocalDate.now();
        long todayNew = candidates.stream().filter(candidate -> today.equals(candidate.getSubmittedDate())).count();
        long pendingFeedback = departmentAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.OPEN)
                .count();
        long timeoutCount = candidates.stream().filter(candidate -> candidate.getStatus() == CandidateStatus.TIMEOUT).count();
        long hiredCount = candidates.stream().filter(candidate -> candidate.getStatus() == CandidateStatus.HIRED).count();
        return new DashboardOverviewResponse(todayNew, pendingFeedback, timeoutCount, hiredCount);
    }

    public List<DashboardMetricResponse> funnel() {
        List<com.recruit.platform.candidate.Candidate> candidates = candidateRepository.findAll();
        return List.of(
                new DashboardMetricResponse("简历投递", candidates.size()),
                new DashboardMetricResponse("部门处理中", countByStatus(candidates, CandidateStatus.IN_DEPT_REVIEW)),
                new DashboardMetricResponse("待面试", countByStatus(candidates, CandidateStatus.PENDING_INTERVIEW)),
                new DashboardMetricResponse("面试中", countByStatus(candidates, CandidateStatus.INTERVIEWING)),
                new DashboardMetricResponse("面试通过", countByStatus(candidates, CandidateStatus.INTERVIEW_PASSED)),
                new DashboardMetricResponse("已录用", countByStatus(candidates, CandidateStatus.HIRED))
        );
    }

    public List<DashboardMetricResponse> statusDistribution() {
        return candidateRepository.findAll().stream()
                .collect(Collectors.groupingBy(candidate -> candidate.getStatus().getLabel(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new DashboardMetricResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<DepartmentEfficiencyResponse> departmentEfficiency() {
        Map<String, List<DepartmentAssignment>> grouped = departmentAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED && assignment.getCompletedAt() != null)
                .collect(Collectors.groupingBy(assignment -> assignment.getDepartment().getName()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    double averageDays = entry.getValue().stream()
                            .mapToLong(assignment -> Duration.between(assignment.getCreatedAt(), assignment.getCompletedAt()).toHours())
                            .average()
                            .orElse(0D) / 24D;
                    return new DepartmentEfficiencyResponse(entry.getKey(), Math.round(averageDays * 10.0) / 10.0, entry.getValue().size());
                })
                .toList();
    }

    public List<AlertResponse> alerts() {
        return departmentAssignmentRepository.findByStatusAndDueAtBefore(AssignmentStatus.OPEN, OffsetDateTime.now()).stream()
                .map(assignment -> new AlertResponse(
                        assignment.getId(),
                        assignment.getCandidate().getId(),
                        assignment.getCandidate().getName(),
                        assignment.getCandidate().getPosition(),
                        assignment.getDepartment().getName(),
                        Math.max(1, Duration.between(assignment.getDueAt(), OffsetDateTime.now()).toHours())
                ))
                .toList();
    }

    private long countByStatus(List<com.recruit.platform.candidate.Candidate> candidates, CandidateStatus status) {
        return candidates.stream().filter(candidate -> candidate.getStatus() == status).count();
    }
}
