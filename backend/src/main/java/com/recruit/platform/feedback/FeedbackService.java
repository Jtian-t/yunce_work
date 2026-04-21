package com.recruit.platform.feedback;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.FeedbackDecision;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.notification.NotificationService;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
import com.recruit.platform.workflow.DepartmentAssignment;
import com.recruit.platform.workflow.WorkflowService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final DepartmentFeedbackRepository feedbackRepository;
    private final WorkflowService workflowService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest request) {
        DepartmentAssignment assignment = workflowService.getAssignment(request.assignmentId());
        User actor = currentUserService.getRequiredUser();
        if (!currentUserService.hasAnyRole(RoleType.ADMIN, RoleType.HR)
                && !assignment.getReviewer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Only the assigned reviewer can submit feedback");
        }

        DepartmentFeedback feedback = new DepartmentFeedback();
        feedback.setAssignment(assignment);
        feedback.setCandidate(assignment.getCandidate());
        feedback.setReviewer(actor);
        feedback.setDecision(request.decision());
        feedback.setFeedback(request.feedback());
        feedback.setRejectReason(request.rejectReason());
        feedback.setNextStep(request.nextStep());
        feedback.setSuggestedInterviewer(request.suggestedInterviewer());
        DepartmentFeedback saved = feedbackRepository.save(feedback);

        workflowService.markCompleted(assignment);
        Candidate candidate = assignment.getCandidate();
        if (request.decision() == FeedbackDecision.PASS) {
            candidate.setStatus(CandidateStatus.PENDING_INTERVIEW);
            candidate.setNextAction(request.nextStep() == null || request.nextStep().isBlank() ? "安排面试" : request.nextStep());
            workflowService.recordStatusChange(candidate, CandidateStatus.PENDING_INTERVIEW, "提交筛选反馈",
                    "部门反馈通过，待安排面试");
            if (candidate.getOwner() != null) {
                notificationService.create(candidate.getOwner(), NotificationType.TASK_COMPLETED,
                        "部门筛选已完成", candidate.getName() + " 已通过部门筛选，请安排后续流程。");
            }
        } else {
            candidate.setStatus(CandidateStatus.REJECTED);
            candidate.setNextAction("-");
            workflowService.recordStatusChange(candidate, CandidateStatus.REJECTED, "提交筛选反馈",
                    "部门反馈未通过：" + (request.rejectReason() == null ? "未说明原因" : request.rejectReason()));
        }
        return toResponse(saved);
    }

    public List<FeedbackResponse> listByCandidate(Long candidateId) {
        return feedbackRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FeedbackResponse toResponse(DepartmentFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getCandidate().getId(),
                feedback.getAssignment().getId(),
                feedback.getReviewer().getDisplayName(),
                feedback.getDecision().name(),
                feedback.getFeedback(),
                feedback.getRejectReason(),
                feedback.getNextStep(),
                feedback.getSuggestedInterviewer(),
                feedback.getCreatedAt()
        );
    }
}
