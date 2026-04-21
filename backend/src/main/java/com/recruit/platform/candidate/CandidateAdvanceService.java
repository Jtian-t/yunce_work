package com.recruit.platform.candidate;

import com.recruit.platform.common.ApiException;
import com.recruit.platform.common.enums.AssignmentStatus;
import com.recruit.platform.common.enums.CandidateAdvanceAction;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.interview.InterviewService;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.workflow.DepartmentAssignment;
import com.recruit.platform.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateAdvanceService {

    private final CandidateService candidateService;
    private final WorkflowService workflowService;
    private final InterviewService interviewService;
    private final CurrentUserService currentUserService;

    public CandidateDetailResponse advance(Long candidateId, AdvanceCandidateRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);

        switch (request.action()) {
            case ASSIGN_TO_DEPARTMENT -> assignToDepartment(candidateId, candidate, request);
            case MOVE_TO_POOL -> moveToPool(candidate);
            case REMIND_REVIEWER -> remindReviewer(candidateId);
            case SCHEDULE_INTERVIEW -> scheduleInterview(candidateId, candidate, request);
            case ADVANCE_TO_OFFER_PENDING -> advanceOfferPending(candidate);
            case MARK_OFFER_SENT -> markOfferSent(candidate);
            case MARK_HIRED -> markHired(candidate);
            case MARK_REJECTED -> markRejected(candidate, request.note());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_ACTION", "Unsupported action");
        }

        return candidateService.get(candidateId);
    }

    private void assignToDepartment(Long candidateId, Candidate candidate, AdvanceCandidateRequest request) {
        requireState(candidate, CandidateAdvanceAction.ASSIGN_TO_DEPARTMENT,
                CandidateStatus.NEW, CandidateStatus.TIMEOUT, CandidateStatus.IN_DEPT_REVIEW, CandidateStatus.PENDING_DEPT_REVIEW);
        if (request.departmentId() == null || request.reviewerId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Department and reviewer are required");
        }
        workflowService.closeOpenAssignments(candidateId);
        workflowService.assignDirect(candidateId, request.departmentId(), request.reviewerId(), request.dueAt());
    }

    private void moveToPool(Candidate candidate) {
        if (candidate.getStatus() == CandidateStatus.HIRED || candidate.getStatus() == CandidateStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Closed candidates cannot move back to pool");
        }
        workflowService.closeOpenAssignments(candidate.getId());
        candidate.setDepartment(null);
        candidate.setOwner(currentUserService.getRequiredUser());
        candidate.setStatus(CandidateStatus.NEW);
        candidate.setNextAction("待 HR 初筛 / 待分发");
        candidateService.recordWorkflowEvent(candidate, "MOVED_TO_POOL", "退回简历池",
                requestNote("候选人已退回简历池", null));
    }

    private void remindReviewer(Long candidateId) {
        DepartmentAssignment assignment = workflowService.getLatestAssignmentForCandidate(candidateId);
        if (assignment.getStatus() != AssignmentStatus.OPEN && assignment.getStatus() != AssignmentStatus.TIMED_OUT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Latest assignment cannot be reminded");
        }
        workflowService.remind(assignment.getId());
    }

    private void scheduleInterview(Long candidateId, Candidate candidate, AdvanceCandidateRequest request) {
        requireState(candidate, CandidateAdvanceAction.SCHEDULE_INTERVIEW,
                CandidateStatus.PENDING_INTERVIEW, CandidateStatus.INTERVIEWING, CandidateStatus.INTERVIEW_PASSED);
        if (request.interviewerId() == null || request.roundLabel() == null || request.scheduledAt() == null || request.endsAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Interview fields are required");
        }
        if (interviewService.hasPendingOrMissingEvaluations(candidateId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INTERVIEW_FLOW_INVALID",
                    "Previous interview is not completed or evaluation is missing");
        }
        interviewService.schedule(
                candidateId,
                request.interviewerId(),
                request.roundLabel(),
                request.scheduledAt(),
                request.endsAt(),
                request.meetingType(),
                request.meetingUrl(),
                request.meetingId(),
                request.meetingPassword(),
                request.interviewStageCode(),
                request.interviewStageLabel(),
                request.interviewDepartmentId(),
                request.interviewNotes()
        );
    }

    private void advanceOfferPending(Candidate candidate) {
        requireState(candidate, CandidateAdvanceAction.ADVANCE_TO_OFFER_PENDING, CandidateStatus.INTERVIEW_PASSED);
        if (interviewService.hasPendingOrMissingEvaluations(candidate.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INTERVIEW_FLOW_INVALID",
                    "Cannot advance to offer when interview evaluations are incomplete");
        }
        candidate.setStatus(CandidateStatus.OFFER_PENDING);
        candidate.setNextAction("待发 Offer");
        candidateService.recordWorkflowEvent(candidate, "ADVANCED_TO_OFFER_PENDING", "推进 Offer 阶段", "候选人已进入 Offer 阶段");
    }

    private void markOfferSent(Candidate candidate) {
        requireState(candidate, CandidateAdvanceAction.MARK_OFFER_SENT, CandidateStatus.OFFER_PENDING);
        candidate.setStatus(CandidateStatus.OFFER_SENT);
        candidate.setNextAction("等待候选人确认 Offer");
        candidateService.recordWorkflowEvent(candidate, "OFFER_SENT", "发放 Offer", "Offer 已发送给候选人");
    }

    private void markHired(Candidate candidate) {
        requireState(candidate, CandidateAdvanceAction.MARK_HIRED, CandidateStatus.OFFER_SENT);
        candidate.setStatus(CandidateStatus.HIRED);
        candidate.setNextAction("-");
        candidateService.recordWorkflowEvent(candidate, "CANDIDATE_HIRED", "标记录用", "候选人已确认入职");
    }

    private void markRejected(Candidate candidate, String note) {
        if (candidate.getStatus() == CandidateStatus.HIRED || candidate.getStatus() == CandidateStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Candidate is already closed");
        }
        workflowService.closeOpenAssignments(candidate.getId());
        candidate.setStatus(CandidateStatus.REJECTED);
        candidate.setNextAction("-");
        candidateService.recordWorkflowEvent(candidate, "CANDIDATE_REJECTED", "人工淘汰", requestNote("候选人已被人工淘汰", note));
    }

    private void requireState(Candidate candidate, CandidateAdvanceAction action, CandidateStatus... allowedStatuses) {
        for (CandidateStatus allowedStatus : allowedStatuses) {
            if (candidate.getStatus() == allowedStatus) {
                return;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS",
                "Action " + action.name() + " is not allowed for status " + candidate.getStatus().name());
    }

    private String requestNote(String defaultNote, String note) {
        return note == null || note.isBlank() ? defaultNote : defaultNote + "：" + note;
    }
}
