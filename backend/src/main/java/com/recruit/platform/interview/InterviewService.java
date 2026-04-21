package com.recruit.platform.interview;

import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateService;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.CandidateStatus;
import com.recruit.platform.common.enums.InterviewMeetingType;
import com.recruit.platform.common.enums.InterviewPlanStatus;
import com.recruit.platform.common.enums.InterviewResult;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.department.Department;
import com.recruit.platform.department.DepartmentRepository;
import com.recruit.platform.notification.NotificationService;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
import com.recruit.platform.user.UserRepository;
import com.recruit.platform.workflow.WorkflowService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
    private final DepartmentRepository departmentRepository;
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
        plan.setOrganizer(actor);
        plan.setRoundLabel(request.roundLabel());
        plan.setScheduledAt(request.scheduledAt());
        plan.setEndsAt(request.endsAt());
        plan.setMeetingType(request.meetingType() == null ? InterviewMeetingType.ONSITE : request.meetingType());
        plan.setMeetingUrl(request.meetingUrl());
        plan.setMeetingId(request.meetingId());
        plan.setMeetingPassword(request.meetingPassword());
        plan.setInterviewStageCode(firstNonBlank(request.interviewStageCode(), defaultStageCode(request.roundLabel())));
        plan.setInterviewStageLabel(firstNonBlank(request.interviewStageLabel(), request.roundLabel()));
        plan.setNotes(request.notes());
        plan.setStatus(InterviewPlanStatus.PLANNED);
        plan.setDepartment(resolveDepartment(request.departmentId(), candidate));
        InterviewPlan saved = interviewPlanRepository.save(plan);

        workflowService.recordStatusChange(candidate, CandidateStatus.INTERVIEWING, "安排面试",
                "已安排 " + interviewer.getDisplayName() + " 进行 " + request.roundLabel());
        candidate.setNextAction("等待" + request.roundLabel() + "结果");

        notificationService.create(interviewer, NotificationType.INTERVIEW_ASSIGNED,
                "收到新的面试安排",
                candidate.getName() + " 的 " + request.roundLabel() + " 已安排给您。",
                Map.of(
                        "candidateId", candidate.getId(),
                        "candidateName", candidate.getName(),
                        "position", candidate.getPosition(),
                        "interviewId", saved.getId(),
                        "roundLabel", request.roundLabel(),
                        "scheduledAt", request.scheduledAt().toString(),
                        "meetingUrl", firstNonBlank(request.meetingUrl(), "")
                ));
        return toPlanResponse(saved);
    }

    @Transactional
    public InterviewPlanResponse schedule(Long candidateId, Long interviewerId, String roundLabel,
                                          OffsetDateTime scheduledAt, OffsetDateTime endsAt,
                                          InterviewMeetingType meetingType, String meetingUrl,
                                          String meetingId, String meetingPassword,
                                          String interviewStageCode, String interviewStageLabel,
                                          Long departmentId, String notes) {
        return createPlan(new CreateInterviewPlanRequest(
                candidateId,
                interviewerId,
                roundLabel,
                scheduledAt,
                endsAt,
                meetingType,
                meetingUrl,
                meetingId,
                meetingPassword,
                interviewStageCode,
                interviewStageLabel,
                departmentId,
                notes
        ));
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
            boolean finalStage = isFinalStage(plan);
            if (finalStage) {
                workflowService.recordStatusChange(plan.getCandidate(), CandidateStatus.INTERVIEW_PASSED, "提交面试评价",
                        "终轮面试通过，得分 " + request.score());
                plan.getCandidate().setNextAction("推进 Offer");
            } else {
                workflowService.recordStatusChange(plan.getCandidate(), CandidateStatus.PENDING_INTERVIEW, "提交面试评价",
                        "当前轮面试通过，等待下一轮安排");
                plan.getCandidate().setNextAction("安排下一轮面试");
            }
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

    public List<InterviewPlanResponse> listMine() {
        User actor = currentUserService.getRequiredUser();
        currentUserService.requireAnyRole(RoleType.INTERVIEWER, RoleType.HR, RoleType.ADMIN);
        if (currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            return interviewPlanRepository.findAllByOrderByScheduledAtAsc().stream()
                    .map(this::toPlanResponse)
                    .toList();
        }
        return interviewPlanRepository.findByInterviewerIdOrderByScheduledAtAsc(actor.getId()).stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public boolean hasPendingOrMissingEvaluations(Long candidateId) {
        List<InterviewPlan> plans = interviewPlanRepository.findByCandidateIdOrderByScheduledAtAsc(candidateId);
        if (plans.isEmpty()) {
            return false;
        }
        for (InterviewPlan plan : plans) {
            boolean hasEvaluation = !interviewEvaluationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                    .filter(evaluation -> evaluation.getInterviewPlan().getId().equals(plan.getId()))
                    .toList()
                    .isEmpty();
            if (plan.getStatus() != InterviewPlanStatus.COMPLETED || !hasEvaluation) {
                return true;
            }
        }
        return false;
    }

    private InterviewPlanResponse toPlanResponse(InterviewPlan plan) {
        List<InterviewEvaluationResponse> evaluations = interviewEvaluationRepository
                .findByCandidateIdOrderByCreatedAtDesc(plan.getCandidate().getId()).stream()
                .filter(evaluation -> evaluation.getInterviewPlan().getId().equals(plan.getId()))
                .map(this::toEvaluationResponse)
                .toList();
        return new InterviewPlanResponse(
                plan.getId(),
                plan.getCandidate().getId(),
                plan.getRoundLabel(),
                plan.getInterviewer().getDisplayName(),
                plan.getCandidate().getName(),
                plan.getCandidate().getPosition(),
                plan.getStatus().name(),
                plan.getScheduledAt(),
                plan.getEndsAt(),
                safeMeetingType(plan),
                plan.getMeetingUrl(),
                plan.getMeetingId(),
                plan.getMeetingPassword(),
                safeStageCode(plan),
                safeStageLabel(plan),
                plan.getOrganizer() == null ? null : plan.getOrganizer().getDisplayName(),
                plan.getDepartment() == null ? null : plan.getDepartment().getId(),
                plan.getDepartment() == null ? null : plan.getDepartment().getName(),
                plan.getNotes(),
                !evaluations.isEmpty(),
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

    private Department resolveDepartment(Long requestDepartmentId, Candidate candidate) {
        if (requestDepartmentId != null) {
            return departmentRepository.findById(requestDepartmentId)
                    .orElseThrow(() -> new NotFoundException("Department not found"));
        }
        return candidate.getDepartment();
    }

    private String defaultStageCode(String roundLabel) {
        String normalized = roundLabel == null ? "" : roundLabel.trim();
        if (normalized.contains("二")) {
            return "ROUND_2";
        }
        if (normalized.contains("三")) {
            return "ROUND_3";
        }
        if (normalized.contains("终")) {
            return "FINAL";
        }
        if (normalized.toUpperCase().contains("HR")) {
            return "HR";
        }
        return "ROUND_1";
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private boolean isFinalStage(InterviewPlan plan) {
        String code = safeStageCode(plan).toUpperCase();
        String label = safeStageLabel(plan);
        return code.contains("FINAL")
                || code.contains("HR")
                || code.contains("ROUND_3")
                || label.contains("终")
                || label.toUpperCase().contains("HR");
    }

    private String safeMeetingType(InterviewPlan plan) {
        return plan.getMeetingType() == null ? InterviewMeetingType.ONSITE.name() : plan.getMeetingType().name();
    }

    private String safeStageCode(InterviewPlan plan) {
        return plan.getInterviewStageCode() == null || plan.getInterviewStageCode().isBlank()
                ? defaultStageCode(plan.getRoundLabel())
                : plan.getInterviewStageCode();
    }

    private String safeStageLabel(InterviewPlan plan) {
        return plan.getInterviewStageLabel() == null || plan.getInterviewStageLabel().isBlank()
                ? plan.getRoundLabel()
                : plan.getInterviewStageLabel();
    }
}
