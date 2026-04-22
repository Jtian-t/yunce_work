package com.recruit.platform.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final ResumeExtractionPipeline resumeExtractionPipeline;
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

        RemoteCandidateInfo remoteCandidate = parsePdfAgentClient.post(
                agentProperties.parsePath(),
                Map.of("resume_text", resumeText),
                RemoteCandidateInfo.class
        );

        ParseReportResponse parseReport = toParseReport(remoteCandidate, resumeText);
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

        ParseReportResponse parseReport = payload.get("parseReport") == null
                ? toParseReport(parseRemoteCandidateInfo(resumeText), resumeText)
                : objectMapper.convertValue(payload.get("parseReport"), ParseReportResponse.class);

        String jobRequirements = firstNonBlank(
                stringValue(payload.get("jdSummary")),
                stringValue(payload.get("focusHint")),
                stringValue(payload.get("targetPosition")),
                stringValue(payload.get("position")),
                candidate.getPosition()
        );

        RemoteAnalysisResult remoteAnalysis = parsePdfAgentClient.post(
                agentProperties.analyzePath(),
                Map.of(
                        "candidate_info", toRemoteCandidateInfo(parseReport),
                        "job_requirements", jobRequirements == null ? "" : jobRequirements,
                        "interview_feedbacks", toRemoteInterviewFeedbacks(payload.get("interviews"))
                ),
                RemoteAnalysisResult.class
        );

        DecisionReportResponse decisionReport = toDecisionReport(parseReport, remoteAnalysis);
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("experienceScore", defaultInt(remoteAnalysis.experienceScore()));
        dimensionScores.put("skillMatchScore", defaultInt(remoteAnalysis.skillMatchScore()));
        dimensionScores.put("overallRecommendation", defaultInt(remoteAnalysis.overallScore()));

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

    private RemoteCandidateInfo parseRemoteCandidateInfo(String resumeText) {
        return parsePdfAgentClient.post(
                agentProperties.parsePath(),
                Map.of("resume_text", resumeText == null ? "" : resumeText),
                RemoteCandidateInfo.class
        );
    }

    private ParseReportResponse toParseReport(RemoteCandidateInfo response, String resumeText) {
        PipelineDocument pipelineDocument = resumeExtractionPipeline.ingest(resumeText);
        List<RemoteEducation> educationItems = safeList(response.education());
        List<RemoteWorkExperience> workExperiences = safeList(response.workExperience());
        List<RemoteProject> projectsFromAgent = safeList(response.projects());
        List<String> skillsFromAgent = safeList(response.skills());

        Map<String, ParseFieldValueResponse> fields = new LinkedHashMap<>();
        putField(fields, "name", response.name(), 0.92, "parse_pdf");
        putField(fields, "phone", response.phone(), 0.92, "parse_pdf");
        putField(fields, "email", response.email(), 0.92, "parse_pdf");
        putField(fields, "education", summarizeEducations(educationItems), 0.88, "parse_pdf");
        putField(fields, "experience", summarizeExperiences(workExperiences), 0.86, "parse_pdf");
        putField(fields, "skillsSummary", skillsFromAgent.isEmpty() ? null : String.join(", ", skillsFromAgent), 0.86, "parse_pdf");
        putField(
                fields,
                "projectSummary",
                projectsFromAgent.stream()
                        .map(RemoteProject::description)
                        .filter(item -> item != null && !item.isBlank())
                        .findFirst()
                        .orElse(null),
                0.82,
                "parse_pdf"
        );

        List<String> highlights = new ArrayList<>();
        if (!skillsFromAgent.isEmpty()) {
            highlights.add("识别出核心技能：" + String.join("、", skillsFromAgent.stream().limit(5).toList()));
        }
        if (!projectsFromAgent.isEmpty()) {
            highlights.add("提取到 " + projectsFromAgent.size() + " 段项目经历");
        }
        if (!workExperiences.isEmpty()) {
            highlights.add("提取到 " + workExperiences.size() + " 段工作经历");
        }
        if (response.summary() != null && !response.summary().isBlank()) {
            highlights.add("生成候选人摘要，可直接用于后续评估");
        }

        List<ParseIssueResponse> issues = new ArrayList<>();
        if (response.phone() == null || response.phone().isBlank()) {
            issues.add(new ParseIssueResponse("WARN", "未识别到手机号，建议人工补录。"));
        }
        if (response.email() == null || response.email().isBlank()) {
            issues.add(new ParseIssueResponse("WARN", "未识别到邮箱，建议人工补录。"));
        }
        if (projectsFromAgent.isEmpty()) {
            issues.add(new ParseIssueResponse("INFO", "项目经历较少，建议人工确认关键项目与职责。"));
        }
        if (pipelineDocument.ocrRequired()) {
            issues.add(new ParseIssueResponse("WARN", "当前文本提取结果为空，可能需要 OCR 处理。"));
        }

        List<ParseProjectResponse> projectExperiences = projectsFromAgent.stream()
                .map(project -> new ParseProjectResponse(
                        firstNonBlank(project.name(), "项目经历"),
                        firstNonBlank(project.description(), project.role(), "")
                ))
                .toList();

        List<ParseProjectDetailResponse> projects = projectsFromAgent.stream()
                .map(project -> new ParseProjectDetailResponse(
                        project.name(),
                        null,
                        project.role(),
                        safeList(project.techStack()),
                        blankToEmptyList(project.description()),
                        List.of(),
                        firstNonBlank(project.description(), "")
                ))
                .toList();

        List<ParseExperienceResponse> experiences = workExperiences.stream()
                .map(item -> new ParseExperienceResponse(item.company(), item.position(), item.duration(), item.description()))
                .toList();

        List<ParseEducationResponse> educations = educationItems.stream()
                .map(item -> new ParseEducationResponse(item.school(), item.degree(), item.duration(), item.major()))
                .toList();

        List<ParseRawBlockResponse> rawBlocks = new ArrayList<>(pipelineDocument.rawBlocks());
        if (response.summary() != null && !response.summary().isBlank()) {
            rawBlocks.add(new ParseRawBlockResponse("SUMMARY", "候选人摘要", response.summary()));
        }

        String summary = firstNonBlank(
                response.summary(),
                "已完成结构化简历解析，识别到 "
                        + skillsFromAgent.size()
                        + " 项技能、"
                        + projectsFromAgent.size()
                        + " 段项目经历和 "
                        + workExperiences.size()
                        + " 段工作经历。"
        );

        return new ParseReportResponse(
                summary,
                highlights,
                skillsFromAgent,
                projectExperiences,
                resumeExtractionPipeline.toStructuredSkills(skillsFromAgent),
                projects,
                experiences,
                educations,
                rawBlocks,
                fields,
                issues,
                pipelineDocument.extractionMode() + "_LLM",
                pipelineDocument.ocrRequired()
        );
    }

    private DecisionReportResponse toDecisionReport(ParseReportResponse parseReport, RemoteAnalysisResult response) {
        int score = defaultInt(response.overallScore());
        String recommendationLevel = score >= 85 ? "强烈推荐" : score >= 70 ? "建议推进" : score >= 55 ? "保守推进" : "暂缓";
        String recommendedAction = score >= 85
                ? "建议推进到下一轮或 Offer 评估阶段，并重点确认入职时间、薪资和团队匹配。"
                : score >= 70
                ? "建议继续推进，同时围绕风险点补充验证。"
                : score >= 55
                ? "建议谨慎推进，优先补充岗位关键能力的验证。"
                : "建议暂缓推进，或转入人才库持续观察。";

        List<String> strengths = new ArrayList<>(parseReport.highlights());
        if (defaultInt(response.skillMatchScore()) >= 80) {
            strengths.add("技能匹配度较高（" + defaultInt(response.skillMatchScore()) + " 分）");
        }
        if (defaultInt(response.experienceScore()) >= 80) {
            strengths.add("经验匹配度较高（" + defaultInt(response.experienceScore()) + " 分）");
        }

        List<String> supportingEvidence = new ArrayList<>();
        supportingEvidence.add("简历摘要：" + parseReport.summary());
        if (response.feedbackSummary() != null && !response.feedbackSummary().isBlank()) {
            supportingEvidence.add("面试反馈总结：" + response.feedbackSummary());
        }

        return new DecisionReportResponse(
                firstNonBlank(response.recommendationReason(), "已完成岗位适配度分析。"),
                score,
                recommendationLevel,
                recommendedAction,
                strengths.stream().distinct().toList(),
                safeList(response.riskPoints()),
                safeList(response.interviewQuestions()),
                supportingEvidence,
                firstNonBlank(response.feedbackSummary(), response.recommendationReason(), "已根据简历与面试反馈生成建议。")
        );
    }

    private RemoteCandidateInfo toRemoteCandidateInfo(ParseReportResponse parseReport) {
        List<RemoteEducation> education = safeList(parseReport.educations()).stream()
                .map(item -> new RemoteEducation(
                        firstNonBlank(item.school(), ""),
                        firstNonBlank(item.summary(), ""),
                        firstNonBlank(item.degree(), ""),
                        firstNonBlank(item.period(), "")
                ))
                .toList();

        List<RemoteWorkExperience> workExperience = safeList(parseReport.experiences()).stream()
                .map(item -> new RemoteWorkExperience(
                        firstNonBlank(item.company(), ""),
                        firstNonBlank(item.role(), ""),
                        firstNonBlank(item.period(), ""),
                        firstNonBlank(item.summary(), "")
                ))
                .toList();

        List<RemoteProject> projects = safeList(parseReport.projects()).stream()
                .map(item -> new RemoteProject(
                        firstNonBlank(item.projectName(), ""),
                        firstNonBlank(item.role(), ""),
                        firstNonBlank(item.summary(), ""),
                        safeList(item.techStack())
                ))
                .toList();

        return new RemoteCandidateInfo(
                firstNonBlank(parseFieldValue(parseReport, "name"), ""),
                firstNonBlank(parseFieldValue(parseReport, "phone"), ""),
                firstNonBlank(parseFieldValue(parseReport, "email"), ""),
                education,
                workExperience,
                projects,
                safeList(parseReport.extractedSkills()),
                parseReport.summary()
        );
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

    private void putField(Map<String, ParseFieldValueResponse> fields, String key, String value, double confidence, String source) {
        if (value == null || value.isBlank()) {
            return;
        }
        fields.put(key, new ParseFieldValueResponse(value, confidence, source));
    }

    private String parseFieldValue(ParseReportResponse parseReport, String key) {
        if (parseReport.fields() == null) {
            return null;
        }
        ParseFieldValueResponse field = parseReport.fields().get(key);
        return field == null ? null : field.value();
    }

    private String summarizeEducations(List<RemoteEducation> education) {
        return education.stream()
                .map(item -> String.join(" ", safeText(item.school()), safeText(item.degree())).trim())
                .filter(item -> !item.isBlank())
                .reduce((left, right) -> left + "；" + right)
                .orElse(null);
    }

    private String summarizeExperiences(List<RemoteWorkExperience> experiences) {
        return experiences.stream()
                .map(item -> firstNonBlank(item.position(), item.company(), item.duration()))
                .filter(item -> item != null && !item.isBlank())
                .reduce((left, right) -> left + "；" + right)
                .orElse(null);
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

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> blankToEmptyList(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }

    private record RemoteEducation(
            String school,
            String major,
            String degree,
            String duration
    ) {
    }

    private record RemoteWorkExperience(
            String company,
            String position,
            String duration,
            String description
    ) {
    }

    private record RemoteProject(
            String name,
            String role,
            String description,
            @JsonProperty("tech_stack")
            List<String> techStack
    ) {
    }

    private record RemoteCandidateInfo(
            String name,
            String phone,
            String email,
            List<RemoteEducation> education,
            @JsonProperty("work_experience")
            List<RemoteWorkExperience> workExperience,
            List<RemoteProject> projects,
            List<String> skills,
            String summary
    ) {
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

    private record RemoteAnalysisResult(
            @JsonProperty("experience_score")
            Integer experienceScore,
            @JsonProperty("skill_match_score")
            Integer skillMatchScore,
            @JsonProperty("overall_score")
            Integer overallScore,
            @JsonProperty("recommendation_reason")
            String recommendationReason,
            @JsonProperty("risk_points")
            List<String> riskPoints,
            @JsonProperty("interview_questions")
            List<String> interviewQuestions,
            @JsonProperty("feedback_summary")
            String feedbackSummary
    ) {
    }
}
