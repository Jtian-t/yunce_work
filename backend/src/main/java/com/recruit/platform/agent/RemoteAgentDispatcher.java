package com.recruit.platform.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.ResumeAsset;
import com.recruit.platform.candidate.ResumeAssetRepository;
import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import com.recruit.platform.config.AppAgentProperties;
import com.recruit.platform.storage.ResumeStorageService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class RemoteAgentDispatcher implements AgentDispatcher {

    private final AgentResultRepository agentResultRepository;
    private final AgentJobRepository agentJobRepository;
    private final ResumeAssetRepository resumeAssetRepository;
    private final ResumeStorageService resumeStorageService;
    private final ObjectMapper objectMapper;
    private final ParsePdfAgentClient parsePdfAgentClient;
    private final AppAgentProperties agentProperties;
    private final LoggingAgentDispatcher fallbackDispatcher;

    @Override
    public void dispatch(AgentJob job, String payloadJson) {
        if (!agentProperties.enabled()) {
            dispatchWithFallback(job, payloadJson, new IllegalStateException("Agent service disabled"));
            return;
        }

        try {
            switch (job.getJobType()) {
                case PARSE -> dispatchParseJob(job, payloadJson);
                case DECISION, ANALYSIS -> dispatchDecisionLikeJob(job, payloadJson);
                default -> dispatchWithFallback(job, payloadJson, new IllegalStateException("Unsupported agent job type"));
            }
        } catch (Exception exception) {
            dispatchWithFallback(job, payloadJson, exception);
        }
    }

    @Transactional
    void dispatchParseJob(AgentJob job, String payloadJson) throws JsonProcessingException, IOException {
        Map<String, Object> payload = readMap(payloadJson);
        ResumeAsset asset = latestResumeAsset(job.getCandidate().getId());
        String resumeText = asset == null ? "" : extractText(asset);

        Map<String, Object> parseRequest = new LinkedHashMap<>();
        parseRequest.put("resume_text", resumeText);
        parseRequest.put("resume_file_url", stringValue(payload.get("resumeFileUrl")));
        parseRequest.put("resume_file_name", firstNonBlank(stringValue(payload.get("originalFileName")), asset == null ? null : asset.getOriginalFileName()));
        parseRequest.put("hint", stringValue(payload.get("hint")));

        ParseReportResponse parseReport = parsePdfAgentClient.post(
                agentProperties.parsePath(),
                parseRequest,
                ParseReportResponse.class
        );

        ParsedCandidateDraftResponse draft = new ParsedCandidateDraftResponse(
                parseFieldValue(parseReport, "name"),
                parseFieldValue(parseReport, "phone"),
                parseFieldValue(parseReport, "email"),
                parseFieldValue(parseReport, "location"),
                parseFieldValue(parseReport, "education"),
                parseFieldValue(parseReport, "experience"),
                parseFieldValue(parseReport, "skillsSummary"),
                parseFieldValue(parseReport, "projectSummary")
        );

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("educationCompleteness", parseReport.educations().isEmpty() ? 35 : 85);
        dimensionScores.put("experienceCompleteness", parseReport.experiences().isEmpty() ? 35 : 85);
        dimensionScores.put("skillsRecognition", Math.min(100, parseReport.extractedSkills().size() * 18 + 20));
        dimensionScores.put("projectRecognition", parseReport.projects().isEmpty() ? 35 : Math.min(100, parseReport.projects().size() * 25 + 35));

        AgentResult result = agentResultRepository.findByJobId(job.getId()).orElseGet(AgentResult::new);
        result.setJob(job);
        result.setSummary(parseReport.summary());
        result.setOverallScore(averageScore(dimensionScores));
        result.setDimensionScoresJson(write(dimensionScores));
        result.setStrengths(joinItems(parseReport.highlights()));
        result.setRisks(joinItems(parseReport.issues().stream().map(ParseIssueResponse::message).toList()));
        result.setRecommendedAction("请确认结构化结果后，再将其用于候选人流程推进。");
        result.setRawReasoningDigest(parseReport.summary());
        result.setParseReportJson(write(parseReport));
        result.setSkillsJson(write(parseReport.skills()));
        result.setProjectsJson(write(parseReport.projects()));
        result.setExperiencesJson(write(parseReport.experiences()));
        result.setEducationsJson(write(parseReport.educations()));
        result.setRawBlocksJson(write(parseReport.rawBlocks()));
        result.setParsedName(draft.name());
        result.setParsedPhone(draft.phone());
        result.setParsedEmail(draft.email());
        result.setParsedLocation(draft.location());
        result.setParsedEducation(draft.education());
        result.setParsedExperience(draft.experience());
        result.setParsedSkillsSummary(draft.skillsSummary());
        result.setParsedProjectSummary(draft.projectSummary());

        markSucceeded(job);
        agentResultRepository.save(result);
    }

    @Transactional
    void dispatchDecisionLikeJob(AgentJob job, String payloadJson) throws JsonProcessingException, IOException {
        Map<String, Object> payload = readMap(payloadJson);
        Candidate candidate = job.getCandidate();
        ResumeAsset asset = latestResumeAsset(candidate.getId());
        String resumeText = asset == null ? "" : extractText(asset);

        ParseReportResponse parseReport;
        if (payload.get("parseReport") == null) {
            Map<String, Object> parseRequest = new LinkedHashMap<>();
            parseRequest.put("resume_text", resumeText);
            parseRequest.put("resume_file_url", stringValue(payload.get("resumeFileUrl")));
            parseRequest.put("resume_file_name", firstNonBlank(stringValue(payload.get("originalFileName")), asset == null ? null : asset.getOriginalFileName()));
            parseReport = parsePdfAgentClient.post(
                    agentProperties.parsePath(),
                    parseRequest,
                    ParseReportResponse.class
            );
        } else {
            parseReport = objectMapper.convertValue(payload.get("parseReport"), ParseReportResponse.class);
        }

        String jobRequirements = firstNonBlank(
                stringValue(payload.get("jdSummary")),
                stringValue(payload.get("focusHint")),
                stringValue(payload.get("targetPosition")),
                stringValue(payload.get("position")),
                candidate.getPosition()
        );

        Map<String, Object> decisionRequest = new LinkedHashMap<>();
        decisionRequest.put("parse_report", parseReport);
        decisionRequest.put("job_requirements", jobRequirements == null ? "" : jobRequirements);
        decisionRequest.put("interview_feedbacks", toRemoteInterviewFeedbacks(payload.get("interviews")));

        DecisionReportResponse decisionReport = parsePdfAgentClient.post(
                agentProperties.analyzePath(),
                decisionRequest,
                DecisionReportResponse.class
        );

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("overallRecommendation", defaultInt(decisionReport.recommendationScore()));
        dimensionScores.put("evidenceCoverage", Math.min(100, safeList(decisionReport.supportingEvidence()).size() * 20 + 20));
        dimensionScores.put("riskControl", Math.max(0, 100 - safeList(decisionReport.risks()).size() * 15));

        AgentResult result = agentResultRepository.findByJobId(job.getId()).orElseGet(AgentResult::new);
        result.setJob(job);
        result.setSummary(decisionReport.conclusion());
        result.setOverallScore(decisionReport.recommendationScore());
        result.setDimensionScoresJson(write(dimensionScores));
        result.setStrengths(joinItems(decisionReport.strengths()));
        result.setRisks(joinItems(decisionReport.risks()));
        result.setRecommendedAction(decisionReport.recommendedAction());
        result.setRawReasoningDigest(decisionReport.reasoningSummary());
        result.setDecisionReportJson(write(decisionReport));
        result.setParseReportJson(write(parseReport));
        result.setSkillsJson(write(parseReport.skills()));
        result.setProjectsJson(write(parseReport.projects()));
        result.setExperiencesJson(write(parseReport.experiences()));
        result.setEducationsJson(write(parseReport.educations()));
        result.setRawBlocksJson(write(parseReport.rawBlocks()));
        result.setParsedName(parseFieldValue(parseReport, "name"));
        result.setParsedPhone(parseFieldValue(parseReport, "phone"));
        result.setParsedEmail(parseFieldValue(parseReport, "email"));
        result.setParsedLocation(parseFieldValue(parseReport, "location"));
        result.setParsedEducation(parseFieldValue(parseReport, "education"));
        result.setParsedExperience(parseFieldValue(parseReport, "experience"));
        result.setParsedSkillsSummary(parseFieldValue(parseReport, "skillsSummary"));
        result.setParsedProjectSummary(parseFieldValue(parseReport, "projectSummary"));

        markSucceeded(job);
        agentResultRepository.save(result);
    }

    private void dispatchWithFallback(AgentJob job, String payloadJson, Exception exception) {
        if (agentProperties.fallbackToMock()) {
            log.warn("Remote agent call failed for job {}, falling back to mock dispatcher", job.getId(), exception);
            fallbackDispatcher.dispatch(job, payloadJson);
            return;
        }

        log.warn("Remote agent call failed for job {}", job.getId(), exception);
        job.setStatus(AgentJobStatus.FAILED);
        job.setStartedAt(OffsetDateTime.now());
        job.setCompletedAt(OffsetDateTime.now());
        job.setLastError(exception.getMessage());
        agentJobRepository.save(job);
    }

    private void markSucceeded(AgentJob job) {
        job.setStatus(AgentJobStatus.SUCCEEDED);
        if (job.getStartedAt() == null) {
            job.setStartedAt(OffsetDateTime.now());
        }
        job.setCompletedAt(OffsetDateTime.now());
        job.setLastError(null);
        agentJobRepository.save(job);
    }

    private ResumeAsset latestResumeAsset(Long candidateId) {
        return resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidateId).orElse(null);
    }

    private List<RemoteInterviewFeedback> toRemoteInterviewFeedbacks(Object interviewsPayload) {
        if (interviewsPayload == null) {
            return List.of();
        }

        List<Map<String, Object>> interviews = objectMapper.convertValue(interviewsPayload, new TypeReference<>() {});
        List<RemoteInterviewFeedback> feedbacks = new ArrayList<>();
        for (Map<String, Object> interview : interviews) {
            Object evaluationsValue = interview.get("evaluations");
            if (evaluationsValue == null) {
                continue;
            }

            List<Map<String, Object>> evaluations = objectMapper.convertValue(evaluationsValue, new TypeReference<>() {});
            for (Map<String, Object> evaluation : evaluations) {
                feedbacks.add(new RemoteInterviewFeedback(
                        roundToNumber(stringValue(interview.get("roundLabel")), stringValue(interview.get("interviewStageLabel"))),
                        stringValue(evaluation.get("interviewer")),
                        stringValue(evaluation.get("evaluation")),
                        toInteger(evaluation.get("score")),
                        splitByDelimiter(stringValue(evaluation.get("strengths"))),
                        splitByDelimiter(stringValue(evaluation.get("weaknesses")))
                ));
            }
        }
        return feedbacks;
    }

    private String extractText(ResumeAsset asset) throws IOException {
        byte[] bytes = readBytes(asset);
        if (looksLikePdf(asset, bytes)) {
            String pdfText = extractPdfText(bytes);
            if (pdfText != null && !pdfText.isBlank()) {
                return pdfText;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readBytes(ResumeAsset asset) throws IOException {
        try (InputStream inputStream = resumeStorageService.openStream(asset.getObjectKey())) {
            return inputStream.readAllBytes();
        }
    }

    private boolean looksLikePdf(ResumeAsset asset, byte[] bytes) {
        if (asset.getContentType() != null && asset.getContentType().toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        if (asset.getOriginalFileName() != null && asset.getOriginalFileName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return true;
        }
        return bytes.length >= 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private Map<String, Object> readMap(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, new TypeReference<>() {});
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize remote agent result", exception);
        }
    }

    private String parseFieldValue(ParseReportResponse parseReport, String key) {
        if (parseReport.fields() == null) {
            return null;
        }
        ParseFieldValueResponse field = parseReport.fields().get(key);
        return field == null ? null : field.value();
    }

    private int averageScore(Map<String, Integer> scores) {
        return scores.isEmpty() ? 0 : (int) Math.round(scores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String joinItems(List<String> items) {
        return safeList(items).stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int roundToNumber(String roundLabel, String stageLabel) {
        String value = firstNonBlank(roundLabel, stageLabel, "");
        if (value.contains("终")) {
            return 5;
        }
        if (value.toUpperCase(Locale.ROOT).contains("HR")) {
            return 4;
        }
        if (value.contains("三")) {
            return 3;
        }
        if (value.contains("二")) {
            return 2;
        }
        return 1;
    }

    private List<String> splitByDelimiter(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，；;\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private record RemoteInterviewFeedback(
            int round,
            String interviewer,
            String feedback,
            Integer score,
            List<String> pros,
            List<String> cons
    ) {
    }
}
