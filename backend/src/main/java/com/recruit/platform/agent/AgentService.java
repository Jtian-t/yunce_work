package com.recruit.platform.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.CandidateService;
import com.recruit.platform.candidate.ResumeAsset;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import com.recruit.platform.common.enums.RoleType;
import com.recruit.platform.config.AppAgentProperties;
import com.recruit.platform.feedback.DepartmentFeedback;
import com.recruit.platform.feedback.DepartmentFeedbackRepository;
import com.recruit.platform.interview.InterviewEvaluation;
import com.recruit.platform.interview.InterviewEvaluationRepository;
import com.recruit.platform.interview.InterviewPlan;
import com.recruit.platform.interview.InterviewPlanRepository;
import com.recruit.platform.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final CandidateService candidateService;
    private final AgentJobRepository agentJobRepository;
    private final AgentResultRepository agentResultRepository;
    private final CurrentUserService currentUserService;
    private final AgentDispatcher agentDispatcher;
    private final ObjectMapper objectMapper;
    private final AppAgentProperties agentProperties;
    private final DepartmentFeedbackRepository feedbackRepository;
    private final InterviewPlanRepository interviewPlanRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;

    @Transactional
    public AgentJobResponse createAnalysisJob(Long candidateId, CreateAnalysisJobRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        ResumeAsset resumeAsset = requireLatestResume(candidateId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateId", candidate.getId());
        payload.put("resumeFileUrl", agentProperties.callbackBaseUrl() + "/api/candidates/" + candidate.getId() + "/resume/download");
        payload.put("targetPosition", candidate.getPosition());
        payload.put("department", candidate.getDepartment() == null ? null : candidate.getDepartment().getName());
        payload.put("jdSummary", firstNonBlank(request.jdSummary(), candidate.getJdSummary(), candidate.getPosition()));
        payload.put("objectKey", resumeAsset.getObjectKey());
        payload.put("originalFileName", resumeAsset.getOriginalFileName());

        AgentJob saved = createJob(candidate, AgentJobType.ANALYSIS, payload);
        agentDispatcher.dispatch(saved, saved.getRequestPayloadJson());
        return toResponse(saved);
    }

    @Transactional
    public AgentJobResponse createParseJob(Long candidateId, CreateParseJobRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        ResumeAsset resumeAsset = requireLatestResume(candidateId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateId", candidate.getId());
        payload.put("objectKey", resumeAsset.getObjectKey());
        payload.put("originalFileName", resumeAsset.getOriginalFileName());
        payload.put("contentType", resumeAsset.getContentType());
        payload.put("resumeFileUrl", agentProperties.callbackBaseUrl() + "/api/candidates/" + candidate.getId() + "/resume/preview");
        payload.put("hint", request.hint());
        payload.put("fallbackName", candidate.getName());
        payload.put("fallbackPhone", candidate.getPhone());
        payload.put("fallbackEmail", candidate.getEmail());
        payload.put("fallbackLocation", candidate.getLocation());
        payload.put("fallbackEducation", candidate.getEducation());
        payload.put("fallbackExperience", candidate.getExperience());
        payload.put("fallbackSkillsSummary", candidate.getSkillsSummary());
        payload.put("fallbackProjectSummary", candidate.getProjectSummary());

        AgentJob saved = createJob(candidate, AgentJobType.PARSE, payload);
        agentDispatcher.dispatch(saved, saved.getRequestPayloadJson());
        return toResponse(saved);
    }

    @Transactional
    public AgentJobResponse createDecisionJob(Long candidateId, CreateDecisionJobRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        ResumeAsset resumeAsset = requireLatestResume(candidateId);
        List<DepartmentFeedback> feedbacks = feedbackRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);
        List<InterviewPlan> interviews = interviewPlanRepository.findByCandidateIdOrderByScheduledAtAsc(candidateId);
        List<InterviewEvaluation> evaluations = interviewEvaluationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);

        Map<Long, List<InterviewEvaluation>> evaluationsByPlanId = evaluations.stream()
                .collect(Collectors.groupingBy(
                        evaluation -> evaluation.getInterviewPlan().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateId", candidate.getId());
        payload.put("focusHint", request.focusHint());
        payload.put("statusCode", candidate.getStatus().getCode());
        payload.put("statusLabel", candidate.getStatus().getLabel());
        payload.put("position", candidate.getPosition());
        payload.put("jdSummary", firstNonBlank(candidate.getJdSummary(), candidate.getPosition()));
        payload.put("nextAction", candidate.getNextAction());
        payload.put("department", candidate.getDepartment() == null ? null : candidate.getDepartment().getName());
        payload.put("resumeFileUrl", agentProperties.callbackBaseUrl() + "/api/candidates/" + candidate.getId() + "/resume/preview");
        payload.put("objectKey", resumeAsset.getObjectKey());
        payload.put("originalFileName", resumeAsset.getOriginalFileName());
        payload.put("contentType", resumeAsset.getContentType());
        payload.put("parseReport", latestParseReport(candidateId).orElseGet(() -> fallbackParseReport(candidate)));
        payload.put("feedbacks", feedbacks.stream().map(this::toFeedbackPayload).toList());
        payload.put("interviews", interviews.stream()
                .map(interview -> toInterviewPayload(interview, evaluationsByPlanId.getOrDefault(interview.getId(), List.of())))
                .toList());

        AgentJob saved = createJob(candidate, AgentJobType.DECISION, payload);
        agentDispatcher.dispatch(saved, saved.getRequestPayloadJson());
        return toResponse(saved);
    }

    public AgentJobResponse latestAnalysis(Long candidateId) {
        return latestByType(candidateId, AgentJobType.ANALYSIS);
    }

    public AgentJobResponse latestParse(Long candidateId) {
        return latestByType(candidateId, AgentJobType.PARSE);
    }

    public AgentJobResponse latestDecision(Long candidateId) {
        return latestByType(candidateId, AgentJobType.DECISION);
    }

    @Transactional
    public void applyParsedProfile(Long candidateId, ApplyParsedProfileRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        ParseReportResponse parseReport = latestParseReport(candidateId)
                .orElseThrow(() -> new NotFoundException("Parse report not found"));
        Set<String> requested = request.fields() == null || request.fields().isEmpty()
                ? Set.of("name", "phone", "email", "location", "education", "experience", "skillsSummary", "projectSummary")
                : request.fields();
        Set<String> locked = readLockedFields(candidate);

        if (requested.contains("name") && !locked.contains("name")) {
            candidate.setName(fieldValue(parseReport, "name", candidate.getName()));
        }
        if (requested.contains("phone") && !locked.contains("phone")) {
            candidate.setPhone(fieldValue(parseReport, "phone", candidate.getPhone()));
        }
        if (requested.contains("email") && !locked.contains("email")) {
            candidate.setEmail(fieldValue(parseReport, "email", candidate.getEmail()));
        }
        if (requested.contains("location") && !locked.contains("location")) {
            candidate.setLocation(fieldValue(parseReport, "location", candidate.getLocation()));
        }
        if (requested.contains("education") && !locked.contains("education")) {
            candidate.setEducation(fieldValue(parseReport, "education", candidate.getEducation()));
        }
        if (requested.contains("experience") && !locked.contains("experience")) {
            candidate.setExperience(fieldValue(parseReport, "experience", candidate.getExperience()));
        }
        if (requested.contains("skillsSummary") && !locked.contains("skillsSummary")) {
            candidate.setSkillsSummary(fieldValue(parseReport, "skillsSummary", candidate.getSkillsSummary()));
        }
        if (requested.contains("projectSummary") && !locked.contains("projectSummary")) {
            candidate.setProjectSummary(fieldValue(parseReport, "projectSummary", candidate.getProjectSummary()));
        }
    }

    @Transactional
    public void saveManualParsedFields(Long candidateId, ParsedProfileManualFieldsRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        if (request.name() != null) {
            candidate.setName(request.name());
        }
        if (request.phone() != null) {
            candidate.setPhone(request.phone());
        }
        if (request.email() != null) {
            candidate.setEmail(request.email());
        }
        if (request.location() != null) {
            candidate.setLocation(request.location());
        }
        if (request.education() != null) {
            candidate.setEducation(request.education());
        }
        if (request.experience() != null) {
            candidate.setExperience(request.experience());
        }
        if (request.skillsSummary() != null) {
            candidate.setSkillsSummary(request.skillsSummary());
        }
        if (request.projectSummary() != null) {
            candidate.setProjectSummary(request.projectSummary());
        }
        // Manual fields are considered locked and should not be overwritten by auto apply.
        Set<String> locked = Set.of("name", "phone", "email", "location", "education", "experience", "skillsSummary", "projectSummary");
        candidate.setParsedFieldLocksJson(write(locked));
    }

    public List<AgentJobResponse> decisionHistory(Long candidateId) {
        return agentJobRepository.findByCandidateIdAndJobTypeOrderByCreatedAtDesc(candidateId, AgentJobType.DECISION)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AgentJobResponse handleAnalysisCallback(Long jobId, String token, AgentCallbackRequest request) {
        return handleCallback(jobId, token, request, AgentJobType.ANALYSIS);
    }

    @Transactional
    public AgentJobResponse handleParseCallback(Long jobId, String token, AgentCallbackRequest request) {
        return handleCallback(jobId, token, request, AgentJobType.PARSE);
    }

    @Transactional
    public AgentJobResponse handleDecisionCallback(Long jobId, String token, AgentCallbackRequest request) {
        return handleCallback(jobId, token, request, AgentJobType.DECISION);
    }

    private AgentJobResponse handleCallback(Long jobId, String token, AgentCallbackRequest request, AgentJobType expectedType) {
        AgentJob job = agentJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Agent job not found"));
        if (!job.getCallbackToken().equals(token)) {
            throw new ForbiddenException("Invalid callback token");
        }
        if (job.getJobType() != expectedType) {
            throw new ForbiddenException("Callback job type does not match endpoint");
        }
        if (request.succeeded()) {
            job.setStatus(AgentJobStatus.SUCCEEDED);
            job.setCompletedAt(OffsetDateTime.now());
            AgentResult result = agentResultRepository.findByJobId(jobId).orElseGet(AgentResult::new);
            result.setJob(job);
            result.setSummary(firstNonBlank(
                    request.summary(),
                    request.parseReport() == null ? null : request.parseReport().summary(),
                    request.decisionReport() == null ? null : request.decisionReport().conclusion()
            ));
            result.setOverallScore(firstNonNull(
                    request.overallScore(),
                    request.decisionReport() == null ? null : request.decisionReport().recommendationScore()
            ));
            result.setDimensionScoresJson(write(request.dimensionScores() == null ? Map.of() : request.dimensionScores()));
            result.setStrengths(firstNonBlank(
                    request.strengths(),
                    request.decisionReport() == null ? null : String.join("；", request.decisionReport().strengths())
            ));
            result.setRisks(firstNonBlank(
                    request.risks(),
                    request.decisionReport() == null ? null : String.join("；", request.decisionReport().risks())
            ));
            result.setRecommendedAction(firstNonBlank(
                    request.recommendedAction(),
                    request.decisionReport() == null ? null : request.decisionReport().recommendedAction()
            ));
            result.setRawReasoningDigest(firstNonBlank(
                    request.rawReasoningDigest(),
                    request.decisionReport() == null ? null : request.decisionReport().reasoningSummary()
            ));
            applyLegacyParsedDraft(result, request);
            if (request.parseReport() != null) {
                result.setParseReportJson(write(request.parseReport()));
                result.setSkillsJson(write(request.parseReport().skills()));
                result.setProjectsJson(write(request.parseReport().projects()));
                result.setExperiencesJson(write(request.parseReport().experiences()));
                result.setEducationsJson(write(request.parseReport().educations()));
                result.setRawBlocksJson(write(request.parseReport().rawBlocks()));
                applyParseReportDraft(result, request.parseReport());
            }
            if (request.decisionReport() != null) {
                result.setDecisionReportJson(write(request.decisionReport()));
            }
            agentResultRepository.save(result);
        } else {
            job.setStatus(AgentJobStatus.FAILED);
            job.setLastError(request.errorMessage());
            job.setCompletedAt(OffsetDateTime.now());
        }
        return toResponse(agentJobRepository.save(job));
    }

    private AgentJob createJob(Candidate candidate, AgentJobType jobType, Map<String, Object> payload) {
        AgentJob job = new AgentJob();
        job.setCandidate(candidate);
        job.setJobType(jobType);
        job.setStatus(AgentJobStatus.PENDING);
        job.setRequestPayloadJson(write(payload));
        job.setCallbackToken(UUID.randomUUID().toString());
        job.setRequestedAt(OffsetDateTime.now());
        return agentJobRepository.save(job);
    }

    private AgentJobResponse latestByType(Long candidateId, AgentJobType jobType) {
        AgentJob job = agentJobRepository.findTopByCandidateIdAndJobTypeOrderByCreatedAtDesc(candidateId, jobType)
                .orElseThrow(() -> new NotFoundException(jobType.name() + " job not found"));
        return toResponse(job);
    }

    private ResumeAsset requireLatestResume(Long candidateId) {
        ResumeAsset resumeAsset = candidateService.getLatestResume(candidateId);
        if (resumeAsset == null) {
            throw new NotFoundException("Resume must be uploaded before creating agent jobs");
        }
        return resumeAsset;
    }

    private Map<String, Object> toFeedbackPayload(DepartmentFeedback feedback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewer", feedback.getReviewer().getDisplayName());
        payload.put("decision", feedback.getDecision().name());
        payload.put("feedback", feedback.getFeedback());
        payload.put("rejectReason", feedback.getRejectReason());
        payload.put("nextStep", feedback.getNextStep());
        payload.put("suggestedInterviewer", feedback.getSuggestedInterviewer());
        payload.put("createdAt", feedback.getCreatedAt());
        return payload;
    }

    private Map<String, Object> toInterviewPayload(InterviewPlan interviewPlan, List<InterviewEvaluation> evaluations) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roundLabel", interviewPlan.getRoundLabel());
        payload.put("interviewer", interviewPlan.getInterviewer().getDisplayName());
        payload.put("status", interviewPlan.getStatus().name());
        payload.put("scheduledAt", interviewPlan.getScheduledAt());
        payload.put("endsAt", interviewPlan.getEndsAt());
        payload.put("meetingType", interviewPlan.getMeetingType().name());
        payload.put("meetingUrl", interviewPlan.getMeetingUrl());
        payload.put("meetingId", interviewPlan.getMeetingId());
        payload.put("meetingPassword", interviewPlan.getMeetingPassword());
        payload.put("interviewStageCode", interviewPlan.getInterviewStageCode());
        payload.put("interviewStageLabel", interviewPlan.getInterviewStageLabel());
        payload.put("evaluations", evaluations.stream().map(evaluation -> {
            Map<String, Object> evaluationPayload = new LinkedHashMap<>();
            evaluationPayload.put("interviewer", evaluation.getInterviewer().getDisplayName());
            evaluationPayload.put("result", evaluation.getResult().name());
            evaluationPayload.put("score", evaluation.getScore());
            evaluationPayload.put("evaluation", evaluation.getEvaluation());
            evaluationPayload.put("strengths", evaluation.getStrengths());
            evaluationPayload.put("weaknesses", evaluation.getWeaknesses());
            evaluationPayload.put("suggestion", evaluation.getSuggestion());
            evaluationPayload.put("createdAt", evaluation.getCreatedAt());
            return evaluationPayload;
        }).toList());
        return payload;
    }

    private Optional<ParseReportResponse> latestParseReport(Long candidateId) {
        return agentJobRepository.findTopByCandidateIdAndJobTypeOrderByCreatedAtDesc(candidateId, AgentJobType.PARSE)
                .flatMap(job -> agentResultRepository.findByJobId(job.getId()))
                .map(this::readParseReport)
                .or(() -> Optional.empty());
    }

    private ParseReportResponse fallbackParseReport(Candidate candidate) {
        Map<String, ParseFieldValueResponse> fields = new LinkedHashMap<>();
        putField(fields, "name", candidate.getName(), 0.4, "candidate_profile");
        putField(fields, "phone", candidate.getPhone(), 0.4, "candidate_profile");
        putField(fields, "email", candidate.getEmail(), 0.4, "candidate_profile");
        putField(fields, "location", candidate.getLocation(), 0.4, "candidate_profile");
        putField(fields, "education", candidate.getEducation(), 0.4, "candidate_profile");
        putField(fields, "experience", candidate.getExperience(), 0.4, "candidate_profile");
        List<String> skills = splitItems(candidate.getSkillsSummary());
        List<ParseProjectResponse> projects = splitItems(candidate.getProjectSummary()).stream()
                .map(item -> new ParseProjectResponse("项目经历", item))
                .toList();
        return new ParseReportResponse(
                "当前尚无新的解析结果，先使用候选人已录入信息作为辅助决策上下文。",
                List.of("使用候选人已有资料作为兜底输入"),
                skills,
                projects,
                skills.stream()
                        .map(skill -> new ParseSkillResponse(skill, skill, "candidate_profile", 0.3))
                        .toList(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                fields,
                List.of(new ParseIssueResponse("INFO", "建议先执行一次简历解析，以获得更完整的结构化结果")),
                "FALLBACK",
                false
        );
    }

    private AgentJobResponse toResponse(AgentJob job) {
        AgentResultResponse resultResponse = agentResultRepository.findByJobId(job.getId())
                .map(result -> {
                    ParseReportResponse parseReport = readParseReport(result);
                    DecisionReportResponse decisionReport = readDecisionReport(result);
                    return new AgentResultResponse(
                            result.getSummary(),
                            result.getOverallScore(),
                            readMap(result.getDimensionScoresJson()),
                            result.getStrengths(),
                            result.getRisks(),
                            result.getRecommendedAction(),
                            result.getRawReasoningDigest(),
                            buildParsedDraft(result, parseReport),
                            parseReport,
                            decisionReport
                    );
                })
                .orElse(null);
        return new AgentJobResponse(
                job.getId(),
                job.getCandidate().getId(),
                job.getJobType(),
                job.getStatus(),
                job.getRequestedAt(),
                job.getCompletedAt(),
                resultResponse,
                job.getLastError()
        );
    }

    private ParsedCandidateDraftResponse buildParsedDraft(AgentResult result, ParseReportResponse parseReport) {
        return new ParsedCandidateDraftResponse(
                fieldValue(parseReport, "name", result.getParsedName()),
                fieldValue(parseReport, "phone", result.getParsedPhone()),
                fieldValue(parseReport, "email", result.getParsedEmail()),
                fieldValue(parseReport, "location", result.getParsedLocation()),
                fieldValue(parseReport, "education", result.getParsedEducation()),
                fieldValue(parseReport, "experience", result.getParsedExperience()),
                fieldValue(parseReport, "skillsSummary", result.getParsedSkillsSummary()),
                fieldValue(parseReport, "projectSummary", result.getParsedProjectSummary())
        );
    }

    private ParseReportResponse readParseReport(AgentResult result) {
        if (result.getParseReportJson() != null && !result.getParseReportJson().isBlank()) {
            return readValue(result.getParseReportJson(), ParseReportResponse.class);
        }
        Map<String, ParseFieldValueResponse> fields = new LinkedHashMap<>();
        putField(fields, "name", result.getParsedName(), 0.35, "legacy_result");
        putField(fields, "phone", result.getParsedPhone(), 0.35, "legacy_result");
        putField(fields, "email", result.getParsedEmail(), 0.35, "legacy_result");
        putField(fields, "location", result.getParsedLocation(), 0.35, "legacy_result");
        putField(fields, "education", result.getParsedEducation(), 0.35, "legacy_result");
        putField(fields, "experience", result.getParsedExperience(), 0.35, "legacy_result");
        putField(fields, "skillsSummary", result.getParsedSkillsSummary(), 0.35, "legacy_result");
        putField(fields, "projectSummary", result.getParsedProjectSummary(), 0.35, "legacy_result");
        if (fields.isEmpty()) {
            return null;
        }
        return new ParseReportResponse(
                firstNonBlank(result.getSummary(), "已读取历史解析结果"),
                List.of("从历史字段回填结构化解析结果"),
                splitItems(result.getParsedSkillsSummary()),
                splitItems(result.getParsedProjectSummary()).stream()
                        .map(item -> new ParseProjectResponse("项目经历", item))
                        .toList(),
                splitItems(result.getParsedSkillsSummary()).stream()
                        .map(item -> new ParseSkillResponse(item, item, "legacy_result", 0.35))
                        .toList(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                fields,
                List.of(),
                "LEGACY",
                false
        );
    }

    private DecisionReportResponse readDecisionReport(AgentResult result) {
        if (result.getDecisionReportJson() != null && !result.getDecisionReportJson().isBlank()) {
            return readValue(result.getDecisionReportJson(), DecisionReportResponse.class);
        }
        if (result.getSummary() == null && result.getRecommendedAction() == null) {
            return null;
        }
        return new DecisionReportResponse(
                firstNonBlank(result.getSummary(), "已生成辅助决策结论"),
                result.getOverallScore(),
                scoreToLevel(result.getOverallScore()),
                result.getRecommendedAction(),
                splitText(result.getStrengths()),
                splitText(result.getRisks()),
                List.of(),
                List.of(),
                result.getRawReasoningDigest()
        );
    }

    private void applyLegacyParsedDraft(AgentResult result, AgentCallbackRequest request) {
        result.setParsedName(firstNonBlank(request.parsedName(), result.getParsedName()));
        result.setParsedPhone(firstNonBlank(request.parsedPhone(), result.getParsedPhone()));
        result.setParsedEmail(firstNonBlank(request.parsedEmail(), result.getParsedEmail()));
        result.setParsedLocation(firstNonBlank(request.parsedLocation(), result.getParsedLocation()));
        result.setParsedEducation(firstNonBlank(request.parsedEducation(), result.getParsedEducation()));
        result.setParsedExperience(firstNonBlank(request.parsedExperience(), result.getParsedExperience()));
        result.setParsedSkillsSummary(firstNonBlank(request.parsedSkillsSummary(), result.getParsedSkillsSummary()));
        result.setParsedProjectSummary(firstNonBlank(request.parsedProjectSummary(), result.getParsedProjectSummary()));
    }

    private void applyParseReportDraft(AgentResult result, ParseReportResponse parseReport) {
        result.setParsedName(fieldValue(parseReport, "name", result.getParsedName()));
        result.setParsedPhone(fieldValue(parseReport, "phone", result.getParsedPhone()));
        result.setParsedEmail(fieldValue(parseReport, "email", result.getParsedEmail()));
        result.setParsedLocation(fieldValue(parseReport, "location", result.getParsedLocation()));
        result.setParsedEducation(fieldValue(parseReport, "education", result.getParsedEducation()));
        result.setParsedExperience(fieldValue(parseReport, "experience", result.getParsedExperience()));
        result.setParsedSkillsSummary(fieldValue(parseReport, "skillsSummary", result.getParsedSkillsSummary()));
        result.setParsedProjectSummary(fieldValue(parseReport, "projectSummary", result.getParsedProjectSummary()));
    }

    private String fieldValue(ParseReportResponse parseReport, String key, String fallback) {
        if (parseReport == null || parseReport.fields() == null) {
            return fallback;
        }
        ParseFieldValueResponse value = parseReport.fields().get(key);
        if (value == null || value.value() == null || value.value().isBlank()) {
            return fallback;
        }
        return value.value();
    }

    private void putField(Map<String, ParseFieldValueResponse> fields, String key, String value, double confidence, String source) {
        if (value == null || value.isBlank()) {
            return;
        }
        fields.put(key, new ParseFieldValueResponse(value, confidence, source));
    }

    private List<String> splitItems(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("[,，;；\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private List<String> splitText(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("[；;\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String scoreToLevel(Integer score) {
        if (score == null) {
            return "待判断";
        }
        if (score >= 85) {
            return "强烈推荐";
        }
        if (score >= 70) {
            return "建议推进";
        }
        if (score >= 55) {
            return "保守推进";
        }
        return "暂缓";
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payload", exception);
        }
    }

    private Map<String, Integer> readMap(String value) {
        try {
            return value == null || value.isBlank() ? Map.of() : objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read analysis result", exception);
        }
    }

    private <T> T readValue(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize stored agent result", exception);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer firstNonNull(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Set<String> readLockedFields(Candidate candidate) {
        try {
            return candidate.getParsedFieldLocksJson() == null || candidate.getParsedFieldLocksJson().isBlank()
                    ? Set.of()
                    : objectMapper.readValue(candidate.getParsedFieldLocksJson(), new TypeReference<>() {
                    });
        } catch (JsonProcessingException exception) {
            return Set.of();
        }
    }
}
