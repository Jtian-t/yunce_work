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
import com.recruit.platform.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

    @Transactional
    public AgentJobResponse createAnalysisJob(Long candidateId, CreateAnalysisJobRequest request) {
        currentUserService.requireAnyRole(RoleType.HR, RoleType.ADMIN);
        Candidate candidate = candidateService.getEntity(candidateId);
        ResumeAsset resumeAsset = candidateService.getLatestResume(candidateId);
        if (resumeAsset == null) {
            throw new NotFoundException("Resume must be uploaded before analysis");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("candidateId", candidate.getId());
        payload.put("resumeFileUrl", agentProperties.callbackBaseUrl() + "/api/candidates/" + candidate.getId() + "/resume/download");
        payload.put("targetPosition", candidate.getPosition());
        payload.put("department", candidate.getDepartment() == null ? null : candidate.getDepartment().getName());
        payload.put("jdSummary", request.jdSummary());
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
        ResumeAsset resumeAsset = candidateService.getLatestResume(candidateId);
        if (resumeAsset == null) {
            throw new NotFoundException("Resume must be uploaded before parsing");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("candidateId", candidate.getId());
        payload.put("objectKey", resumeAsset.getObjectKey());
        payload.put("originalFileName", resumeAsset.getOriginalFileName());
        payload.put("resumeFileUrl", agentProperties.callbackBaseUrl() + "/api/candidates/" + candidate.getId() + "/resume/preview");
        payload.put("hint", request.hint());
        payload.put("fallbackName", candidate.getName());
        payload.put("fallbackPhone", candidate.getPhone());
        payload.put("fallbackEmail", candidate.getEmail());
        payload.put("fallbackEducation", candidate.getEducation());
        payload.put("fallbackExperience", candidate.getExperience());
        payload.put("fallbackSkillsSummary", candidate.getSkillsSummary());
        payload.put("fallbackProjectSummary", candidate.getProjectSummary());

        AgentJob saved = createJob(candidate, AgentJobType.PARSE, payload);
        agentDispatcher.dispatch(saved, saved.getRequestPayloadJson());
        return toResponse(saved);
    }

    public AgentJobResponse latestAnalysis(Long candidateId) {
        AgentJob job = agentJobRepository.findTopByCandidateIdAndJobTypeOrderByCreatedAtDesc(candidateId, AgentJobType.ANALYSIS)
                .orElseThrow(() -> new NotFoundException("Analysis job not found"));
        return toResponse(job);
    }

    public AgentJobResponse latestParse(Long candidateId) {
        AgentJob job = agentJobRepository.findTopByCandidateIdAndJobTypeOrderByCreatedAtDesc(candidateId, AgentJobType.PARSE)
                .orElseThrow(() -> new NotFoundException("Parse job not found"));
        return toResponse(job);
    }

    @Transactional
    public AgentJobResponse handleAnalysisCallback(Long jobId, String token, AgentCallbackRequest request) {
        return handleCallback(jobId, token, request, AgentJobType.ANALYSIS);
    }

    @Transactional
    public AgentJobResponse handleParseCallback(Long jobId, String token, AgentCallbackRequest request) {
        return handleCallback(jobId, token, request, AgentJobType.PARSE);
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
            result.setSummary(request.summary());
            result.setOverallScore(request.overallScore());
            result.setDimensionScoresJson(write(request.dimensionScores()));
            result.setStrengths(request.strengths());
            result.setRisks(request.risks());
            result.setRecommendedAction(request.recommendedAction());
            result.setRawReasoningDigest(request.rawReasoningDigest());
            result.setParsedName(request.parsedName());
            result.setParsedPhone(request.parsedPhone());
            result.setParsedEmail(request.parsedEmail());
            result.setParsedEducation(request.parsedEducation());
            result.setParsedExperience(request.parsedExperience());
            result.setParsedSkillsSummary(request.parsedSkillsSummary());
            result.setParsedProjectSummary(request.parsedProjectSummary());
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

    private AgentJobResponse toResponse(AgentJob job) {
        AgentResultResponse resultResponse = agentResultRepository.findByJobId(job.getId())
                .map(result -> new AgentResultResponse(
                        result.getSummary(),
                        result.getOverallScore(),
                        readMap(result.getDimensionScoresJson()),
                        result.getStrengths(),
                        result.getRisks(),
                        result.getRecommendedAction(),
                        result.getRawReasoningDigest(),
                        new ParsedCandidateDraftResponse(
                                result.getParsedName(),
                                result.getParsedPhone(),
                                result.getParsedEmail(),
                                result.getParsedEducation(),
                                result.getParsedExperience(),
                                result.getParsedSkillsSummary(),
                                result.getParsedProjectSummary()
                        )
                ))
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

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payload", exception);
        }
    }

    private Map<String, Integer> readMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read analysis result", exception);
        }
    }
}
