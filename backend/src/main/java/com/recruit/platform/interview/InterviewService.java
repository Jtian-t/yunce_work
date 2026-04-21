package com.recruit.platform.interview;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateService;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.InterviewPlanStatus;
import com.recruit.platform.common.enums.InterviewResult;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.notification.NotificationService;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import com.recruit.platform.workflow.WorkflowService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewPlanRepository interviewPlanRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final CandidateService candidateService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;

    @Transactional
    public InterviewPlanResponse createPlan(CreateInterviewPlanRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(request.candidateId());
        User interviewer = userRepository.findById(request.interviewerId())
                .orElseThrow(() -> new NotFoundException("Interviewer not found"));
        User actor = currentUserService.getRequiredUser();

        InterviewPlan plan = new InterviewPlan();
        plan.setCandidate(candidate);
        plan.setInterviewer(interviewer);
        plan.setCreatedBy(actor);
        plan.setRoundLabel(request.roundLabel());
        plan.setScheduledAt(request.scheduledAt());
        plan.setEndsAt(request.endsAt());
        plan.setStatus(InterviewPlanStatus.PLANNED);
        InterviewPlan saved = interviewPlanRepository.save(plan);

        workflowService.recordStatusChange(candidate, CandidateStatus.INTERVIEWING, "安排面试",
                "已安排 " + interviewer.getDisplayName() + " 进行 " + request.roundLabel());
        candidate.setNextAction("等待面试结果");
        notificationService.create(interviewer, NotificationType.TASK_ASSIGNED,
                "收到新的面试安排", candidate.getName() + " 的 " + request.roundLabel() + " 已安排给您。");
        return toPlanResponse(saved);
    }

    @Transactional
    public InterviewPlanResponse schedule(Long candidateId, Long interviewerId, String roundLabel,
                                          OffsetDateTime scheduledAt, OffsetDateTime endsAt) {
        return createPlan(new CreateInterviewPlanRequest(candidateId, interviewerId, roundLabel, scheduledAt, endsAt));
    }

    @Transactional
    public InterviewEvaluationResponse submitEvaluation(Long interviewId, SubmitInterviewEvaluationRequest request) {
        InterviewPlan plan = interviewPlanRepository.findById(interviewId)
                .orElseThrow(() -> new NotFoundException("Interview plan not found"));
        User actor = currentUserService.getRequiredUser();
        if (!currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)
                && !plan.getInterviewer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Only the planned interviewer can submit evaluation");
        }

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setInterviewPlan(plan);
        evaluation.setCandidate(plan.getCandidate());
        evaluation.setInterviewer(actor);
        evaluation.setResult(request.result());
        evaluation.setScore(request.score());
        evaluation.setEvaluation(request.evaluation());
        evaluation.setStrengths(request.strengths());
        evaluation.setWeaknesses(request.weaknesses());
        evaluation.setSuggestion(request.suggestion());
        InterviewEvaluation saved = interviewEvaluationRepository.save(evaluation);

        plan.setStatus(InterviewPlanStatus.COMPLETED);
        if (request.result() == InterviewResult.PASS) {
            workflowService.recordStatusChange(plan.getCandidate(), CandidateStatus.INTERVIEW_PASSED, "提交面试评价",
                    "面试通过，得分 " + request.score());
            plan.getCandidate().setNextAction("进入下一轮或推进 Offer");
        } else {
            workflowService.recordStatusChange(plan.getCandidate(), CandidateStatus.REJECTED, "提交面试评价",
                    "面试未通过，得分 " + request.score());
            plan.getCandidate().setNextAction("-");
        }
        return toEvaluationResponse(saved);
    }

    public List<InterviewPlanResponse> listByCandidate(Long candidateId) {
        return interviewPlanRepository.findByCandidateIdOrderByScheduledAtAsc(candidateId).stream()
                .map(this::toPlanResponse)
                .toList();
    }

    private InterviewPlanResponse toPlanResponse(InterviewPlan plan) {
        List<InterviewEvaluationResponse> evaluations = interviewEvaluationRepository.findByCandidateIdOrderByCreatedAtDesc(plan.getCandidate().getId()).stream()
                .filter(evaluation -> evaluation.getInterviewPlan().getId().equals(plan.getId()))
                .map(this::toEvaluationResponse)
                .toList();
        return new InterviewPlanResponse(
                plan.getId(),
                plan.getCandidate().getId(),
                plan.getRoundLabel(),
                plan.getInterviewer().getDisplayName(),
                plan.getStatus().name(),
                plan.getScheduledAt(),
                plan.getEndsAt(),
                evaluations
        );
    }

    private InterviewEvaluationResponse toEvaluationResponse(InterviewEvaluation evaluation) {
        return new InterviewEvaluationResponse(
                evaluation.getId(),
                evaluation.getInterviewer().getDisplayName(),
                evaluation.getResult().name(),
                evaluation.getScore(),
                evaluation.getEvaluation(),
                evaluation.getStrengths(),
                evaluation.getWeaknesses(),
                evaluation.getSuggestion(),
                evaluation.getCreatedAt()
        );
    }
}
