package com.recruit.platform.workflow;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateService;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.notification.NotificationService;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkflowService {

    private final CandidateService candidateService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DepartmentAssignmentRepository departmentAssignmentRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional
    public AssignmentResponse assign(Long candidateId, AssignmentRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFoundException("Department not found"));
        User reviewer = userRepository.findById(request.reviewerId())
                .orElseThrow(() -> new NotFoundException("Reviewer not found"));
        User actor = currentUserService.getRequiredUser();

        candidate.setDepartment(department);
        candidate.setOwner(reviewer);
        candidate.setStatus(CandidateStatus.IN_DEPT_REVIEW);
        candidate.setNextAction("等待部门反馈");

        DepartmentAssignment assignment = new DepartmentAssignment();
        assignment.setCandidate(candidate);
        assignment.setDepartment(department);
        assignment.setReviewer(reviewer);
        assignment.setAssignedBy(actor);
        assignment.setStatus(AssignmentStatus.OPEN);
        assignment.setDueAt(request.dueAt() == null ? OffsetDateTime.now().plusDays(2) : request.dueAt());
        DepartmentAssignment saved = departmentAssignmentRepository.save(assignment);

        createWorkflowEvent(candidate, actor, "ASSIGNED_TO_DEPARTMENT", "分发给部门",
                "已分发给 " + department.getName() + " / " + reviewer.getDisplayName());
        notificationService.create(reviewer, NotificationType.TASK_ASSIGNED,
                "收到新的简历筛选任务", candidate.getName() + " 已分发给您，请尽快反馈。");
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse assignDirect(Long candidateId, Long departmentId, Long reviewerId, OffsetDateTime dueAt) {
        return assign(candidateId, new AssignmentRequest(departmentId, reviewerId, dueAt));
    }

    public List<AssignmentResponse> pendingTasks() {
        User user = currentUserService.getRequiredUser();
        currentUserService.requireAnyRole(RoleType.DEPARTMENT_LEAD, RoleType.HR, RoleType.ADMIN);
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return departmentAssignmentRepository.findAll().stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.OPEN)
                    .map(this::toResponse)
                    .toList();
        }
        return departmentAssignmentRepository.findByReviewerIdAndStatusOrderByDueAtAsc(user.getId(), AssignmentStatus.OPEN)
                .stream().map(this::toResponse).toList();
    }

    public List<AssignmentResponse> completedTasks() {
        User user = currentUserService.getRequiredUser();
        currentUserService.requireAnyRole(RoleType.DEPARTMENT_LEAD, RoleType.HR, RoleType.ADMIN);
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return departmentAssignmentRepository.findAll().stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.COMPLETED)
                    .map(this::toResponse)
                    .toList();
        }
        return departmentAssignmentRepository.findByReviewerIdAndStatusOrderByDueAtAsc(user.getId(), AssignmentStatus.COMPLETED)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssignmentResponse remind(Long assignmentId) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        DepartmentAssignment assignment = departmentAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        assignment.setRemindedAt(OffsetDateTime.now());
        departmentAssignmentRepository.save(assignment);
        notificationService.create(assignment.getReviewer(), NotificationType.REMINDER,
                "简历筛选催办", assignment.getCandidate().getName() + " 的反馈已接近或超过时限，请尽快处理。");
        createWorkflowEvent(assignment.getCandidate(), currentUserService.getRequiredUser(), "REMINDER_SENT", "发送催办",
                "已向 " + assignment.getReviewer().getDisplayName() + " 发送催办通知");
        return toResponse(assignment);
    }

    @Transactional
    public void markCompleted(DepartmentAssignment assignment) {
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(OffsetDateTime.now());
        departmentAssignmentRepository.save(assignment);
    }

    public DepartmentAssignment getAssignment(Long id) {
        return departmentAssignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
    }

    public DepartmentAssignment getLatestAssignmentForCandidate(Long candidateId) {
        return departmentAssignmentRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
    }

    @Transactional
    public void closeOpenAssignments(Long candidateId) {
        for (DepartmentAssignment assignment : departmentAssignmentRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId)) {
            if (assignment.getStatus() == AssignmentStatus.OPEN || assignment.getStatus() == AssignmentStatus.TIMED_OUT) {
                assignment.setStatus(AssignmentStatus.COMPLETED);
                assignment.setCompletedAt(OffsetDateTime.now());
                departmentAssignmentRepository.save(assignment);
            }
        }
    }

    @Transactional
    public void recordStatusChange(Candidate candidate, CandidateStatus status, String sourceAction, String note) {
        candidate.setStatus(status);
        createWorkflowEvent(candidate, currentUserService.getRequiredUser(), "STATUS_CHANGED", sourceAction, note);
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Shanghai")
    @Transactional
    public void markTimeoutAssignments() {
        List<DepartmentAssignment> overdueAssignments = departmentAssignmentRepository
                .findByStatusAndDueAtBefore(AssignmentStatus.OPEN, OffsetDateTime.now());
        for (DepartmentAssignment assignment : overdueAssignments) {
            assignment.setStatus(AssignmentStatus.TIMED_OUT);
            assignment.getCandidate().setStatus(CandidateStatus.TIMEOUT);
            assignment.getCandidate().setNextAction("处理超时反馈");
            createWorkflowEvent(assignment.getCandidate(), assignment.getAssignedBy(), "TIMEOUT", "超时未反馈",
                    assignment.getDepartment().getName() + " 超过截止时间未反馈");
            notificationService.create(assignment.getReviewer(), NotificationType.REMINDER,
                    "任务已超时", assignment.getCandidate().getName() + " 的反馈已超时，请立即处理。");
        }
    }

    private AssignmentResponse toResponse(DepartmentAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCandidate().getId(),
                assignment.getCandidate().getName(),
                assignment.getDepartment().getName(),
                assignment.getReviewer().getDisplayName(),
                assignment.getStatus().name(),
                assignment.getDueAt(),
                assignment.getCompletedAt(),
                assignment.getRemindedAt()
        );
    }

    private void createWorkflowEvent(Candidate candidate, User actor, String eventType, String sourceAction, String note) {
        WorkflowEvent event = new WorkflowEvent();
        event.setCandidate(candidate);
        event.setActorId(actor.getId());
        event.setActorName(actor.getDisplayName());
        event.setEventType(eventType);
        event.setSourceAction(sourceAction);
        event.setStatusCode(candidate.getStatus().getCode());
        event.setStatusLabel(candidate.getStatus().getLabel());
        event.setNote(note);
        event.setOccurredAt(OffsetDateTime.now());
        workflowEventRepository.save(event);
    }
}
