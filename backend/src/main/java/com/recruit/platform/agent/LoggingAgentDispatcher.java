package com.recruit.platform.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.candidate.Candidate;
import com.recruit.platform.candidate.ResumeAsset;
import com.recruit.platform.candidate.ResumeAssetRepository;
import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import com.recruit.platform.feedback.DepartmentFeedback;
import com.recruit.platform.feedback.DepartmentFeedbackRepository;
import com.recruit.platform.interview.InterviewEvaluation;
import com.recruit.platform.interview.InterviewEvaluationRepository;
import com.recruit.platform.interview.InterviewPlan;
import com.recruit.platform.interview.InterviewPlanRepository;
import com.recruit.platform.storage.ResumeStorageService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAgentDispatcher implements AgentDispatcher {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("1\\d{10}");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+\\s*年)");
    private static final List<String> EDUCATION_KEYWORDS = List.of("博士", "硕士", "本科", "大专", "专科");
    private static final List<String> CITY_KEYWORDS = List.of("上海", "北京", "深圳", "广州", "杭州", "成都", "苏州", "南京", "武汉", "西安");
    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring", "MySQL", "Redis", "Kafka", "RabbitMQ", "Docker", "Kubernetes",
            "Vue", "React", "TypeScript", "JavaScript", "Python", "Go", "Elasticsearch", "Linux"
    );

    private final AgentJobRepository agentJobRepository;
    private final AgentResultRepository agentResultRepository;
    private final ResumeAssetRepository resumeAssetRepository;
    private final ResumeStorageService resumeStorageService;
    private final ObjectMapper objectMapper;
    private final DepartmentFeedbackRepository feedbackRepository;
    private final InterviewPlanRepository interviewPlanRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final ResumeExtractionPipeline resumeExtractionPipeline;

    @Override
    public void dispatch(AgentJob job, String payloadJson) {
        log.info("Dispatching agent job {} with payload {}", job.getId(), payloadJson);
        if (job.getJobType() == AgentJobType.PARSE) {
            autoCompleteParseJob(job, payloadJson);
        } else if (job.getJobType() == AgentJobType.DECISION) {
            autoCompleteDecisionJob(job, payloadJson);
        }
    }

    @Transactional
    void autoCompleteParseJob(AgentJob job, String payloadJson) {
        try {
            Map<String, Object> payload = readMap(payloadJson);
            ResumeAsset asset = resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(job.getCandidate().getId()).orElse(null);
            String resumeText = asset == null ? "" : extractText(asset);
            ParseReportResponse parseReport = buildParseReport(payload, resumeText);
            ParsedCandidateDraftResponse draft = buildParsedDraft(parseReport);
            Map<String, Integer> dimensionScores = buildParseDimensionScores(parseReport);

            AgentResult result = agentResultRepository.findByJobId(job.getId()).orElseGet(AgentResult::new);
            result.setJob(job);
            result.setSummary(parseReport.summary());
            result.setOverallScore(averageScore(dimensionScores));
            result.setDimensionScoresJson(write(dimensionScores));
            result.setStrengths(joinItems(parseReport.highlights()));
            result.setRisks(joinItems(parseReport.issues().stream().map(ParseIssueResponse::message).toList()));
            result.setRecommendedAction("请确认解析结果后再推进候选人流程");
            result.setRawReasoningDigest(buildParseDigest(parseReport));
            result.setParseReportJson(write(parseReport));
            result.setSkillsJson(write(parseReport.skills()));
            result.setProjectsJson(write(parseReport.projects()));
            result.setExperiencesJson(write(parseReport.experiences()));
            result.setEducationsJson(write(parseReport.educations()));
            result.setRawBlocksJson(write(parseReport.rawBlocks()));
            applyDraft(result, draft);

            job.setStatus(AgentJobStatus.SUCCEEDED);
            job.setStartedAt(OffsetDateTime.now());
            job.setCompletedAt(OffsetDateTime.now());
            job.setLastError(null);
            agentJobRepository.save(job);
            agentResultRepository.save(result);
        } catch (Exception exception) {
            markFailed(job, exception);
        }
    }

    @Transactional
    void autoCompleteDecisionJob(AgentJob job, String payloadJson) {
        try {
            Map<String, Object> payload = readMap(payloadJson);
            Candidate candidate = job.getCandidate();
            ResumeAsset asset = resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(candidate.getId()).orElse(null);
            ParseReportResponse parseReport = payload.get("parseReport") == null
                    ? buildFallbackParseReport(payload, asset)
                    : objectMapper.convertValue(payload.get("parseReport"), ParseReportResponse.class);

            List<DepartmentFeedback> feedbacks = feedbackRepository.findByCandidateIdOrderByCreatedAtDesc(candidate.getId());
            List<InterviewPlan> interviews = interviewPlanRepository.findByCandidateIdOrderByScheduledAtAsc(candidate.getId());
            List<InterviewEvaluation> evaluations = interviewEvaluationRepository.findByCandidateIdOrderByCreatedAtDesc(candidate.getId());

            DecisionComputation computation = buildDecisionReport(candidate, parseReport, feedbacks, interviews, evaluations);
            AgentResult result = agentResultRepository.findByJobId(job.getId()).orElseGet(AgentResult::new);
            result.setJob(job);
            result.setSummary(computation.report().conclusion());
            result.setOverallScore(computation.report().recommendationScore());
            result.setDimensionScoresJson(write(computation.dimensionScores()));
            result.setStrengths(joinItems(computation.report().strengths()));
            result.setRisks(joinItems(computation.report().risks()));
            result.setRecommendedAction(computation.report().recommendedAction());
            result.setRawReasoningDigest(computation.report().reasoningSummary());
            result.setDecisionReportJson(write(computation.report()));

            job.setStatus(AgentJobStatus.SUCCEEDED);
            job.setStartedAt(OffsetDateTime.now());
            job.setCompletedAt(OffsetDateTime.now());
            job.setLastError(null);
            agentJobRepository.save(job);
            agentResultRepository.save(result);
        } catch (Exception exception) {
            markFailed(job, exception);
        }
    }

    private ParseReportResponse buildParseReport(Map<String, Object> payload, String extractedText) {
        PipelineDocument pipelineDocument = resumeExtractionPipeline.ingest(extractedText);
        String normalized = pipelineDocument.normalizedText();
        boolean ocrRequired = pipelineDocument.ocrRequired();
        String extractionMode = pipelineDocument.extractionMode();
        Map<String, ParseFieldValueResponse> fields = new LinkedHashMap<>();

        captureField(fields, "name", firstNonBlank(
                extractByPrefix(normalized, "姓名"),
                extractByPrefix(normalized, "Name"),
                stringValue(payload.get("fallbackName")),
                fileStem(stringValue(payload.get("originalFileName")))
        ), normalized.contains("姓名") || normalized.contains("Name") ? 0.95 : 0.45,
                normalized.contains("姓名") || normalized.contains("Name") ? "resume_text" : "candidate_fallback");

        captureField(fields, "phone", firstNonBlank(match(normalized, PHONE_PATTERN), stringValue(payload.get("fallbackPhone"))),
                match(normalized, PHONE_PATTERN) != null ? 0.95 : 0.4,
                match(normalized, PHONE_PATTERN) != null ? "resume_text" : "candidate_fallback");

        captureField(fields, "email", firstNonBlank(match(normalized, EMAIL_PATTERN), stringValue(payload.get("fallbackEmail"))),
                match(normalized, EMAIL_PATTERN) != null ? 0.95 : 0.4,
                match(normalized, EMAIL_PATTERN) != null ? "resume_text" : "candidate_fallback");

        captureField(fields, "location", firstNonBlank(extractLocation(normalized), stringValue(payload.get("fallbackLocation"))),
                extractLocation(normalized) != null ? 0.8 : 0.35,
                extractLocation(normalized) != null ? "resume_text" : "candidate_fallback");

        captureField(fields, "education", firstNonBlank(extractEducation(normalized), stringValue(payload.get("fallbackEducation"))),
                extractEducation(normalized) != null ? 0.85 : 0.4,
                extractEducation(normalized) != null ? "resume_text" : "candidate_fallback");

        captureField(fields, "experience", firstNonBlank(match(normalized, EXPERIENCE_PATTERN), stringValue(payload.get("fallbackExperience"))),
                match(normalized, EXPERIENCE_PATTERN) != null ? 0.85 : 0.4,
                match(normalized, EXPERIENCE_PATTERN) != null ? "resume_text" : "candidate_fallback");

        captureField(fields, "recentCompany", extractByPrefix(normalized, "最近公司"), 0.7, "resume_text");
        captureField(fields, "recentRole", firstNonBlank(extractByPrefix(normalized, "最近岗位"), extractByPrefix(normalized, "应聘岗位")), 0.7, "resume_text");

        List<String> skills = extractSkills(normalized, stringValue(payload.get("fallbackSkillsSummary")));
        if (!skills.isEmpty()) {
            captureField(fields, "skillsSummary", String.join(", ", skills), 0.75, "resume_text");
        } else {
            captureField(fields, "skillsSummary", stringValue(payload.get("fallbackSkillsSummary")), 0.35, "candidate_fallback");
        }

        List<ParseProjectResponse> projects = extractProjects(normalized, stringValue(payload.get("fallbackProjectSummary")));
        if (!projects.isEmpty()) {
            captureField(fields, "projectSummary", projects.stream().map(ParseProjectResponse::summary).collect(Collectors.joining("；")), 0.7, "resume_text");
        } else {
            captureField(fields, "projectSummary", stringValue(payload.get("fallbackProjectSummary")), 0.35, "candidate_fallback");
        }

        List<String> highlights = new ArrayList<>();
        if (!skills.isEmpty()) {
            highlights.add("识别出 " + skills.size() + " 项核心技能：" + String.join("、", skills.stream().limit(4).toList()));
        }
        if (!projects.isEmpty()) {
            highlights.add("提取到 " + projects.size() + " 段项目/职责描述");
        }
        if (fields.containsKey("experience")) {
            highlights.add("识别到候选人经验信息：" + fields.get("experience").value());
        }
        if (fields.containsKey("education")) {
            highlights.add("学历信息完整：" + fields.get("education").value());
        }

        List<ParseIssueResponse> issues = new ArrayList<>();
        if (!fields.containsKey("phone")) {
            issues.add(new ParseIssueResponse("WARN", "未识别到手机号，建议人工补录"));
        }
        if (!fields.containsKey("email")) {
            issues.add(new ParseIssueResponse("WARN", "未识别到邮箱，建议人工补录"));
        }
        if (skills.size() < 2) {
            issues.add(new ParseIssueResponse("INFO", "技能标签较少，建议补充技术栈关键词"));
        }
        if (projects.isEmpty()) {
            issues.add(new ParseIssueResponse("WARN", "项目经历提取不足，建议补充项目职责或成果"));
        }
        if (normalized.isBlank()) {
            issues.add(new ParseIssueResponse("WARN", "当前简历文本提取结果为空，可能是扫描 PDF，建议走 OCR 分支"));
        }

        String summary = buildParseSummary(fields, skills, projects, issues);
        List<ParseSkillResponse> structuredSkills = resumeExtractionPipeline.toStructuredSkills(skills);
        List<ParseProjectDetailResponse> structuredProjects = resumeExtractionPipeline.toStructuredProjects(projects);
        List<ParseRawBlockResponse> rawBlocks = new ArrayList<>(pipelineDocument.rawBlocks());
        projects.forEach(project -> rawBlocks.add(new ParseRawBlockResponse("PROJECT", project.title(), project.summary())));
        return new ParseReportResponse(
                summary,
                highlights,
                skills,
                projects,
                structuredSkills,
                structuredProjects,
                List.of(),
                List.of(),
                rawBlocks,
                fields,
                issues,
                extractionMode,
                ocrRequired
        );
    }

    private ParsedCandidateDraftResponse buildParsedDraft(ParseReportResponse parseReport) {
        return new ParsedCandidateDraftResponse(
                fieldValue(parseReport, "name"),
                fieldValue(parseReport, "phone"),
                fieldValue(parseReport, "email"),
                fieldValue(parseReport, "location"),
                fieldValue(parseReport, "education"),
                fieldValue(parseReport, "experience"),
                fieldValue(parseReport, "skillsSummary"),
                fieldValue(parseReport, "projectSummary")
        );
    }

    private Map<String, Integer> buildParseDimensionScores(ParseReportResponse parseReport) {
        int contact = 0;
        if (fieldValue(parseReport, "phone") != null) {
            contact += 50;
        }
        if (fieldValue(parseReport, "email") != null) {
            contact += 50;
        }
        int profile = 0;
        if (fieldValue(parseReport, "education") != null) {
            profile += 50;
        }
        if (fieldValue(parseReport, "experience") != null) {
            profile += 50;
        }
        int skills = Math.min(100, parseReport.extractedSkills().size() * 20 + (parseReport.extractedSkills().isEmpty() ? 0 : 20));
        int projects = parseReport.projectExperiences().isEmpty() ? 20 : Math.min(100, parseReport.projectExperiences().size() * 30 + 40);
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("contactCompleteness", contact);
        dimensionScores.put("profileCompleteness", profile);
        dimensionScores.put("skillsRecognition", skills);
        dimensionScores.put("projectRecognition", projects);
        return dimensionScores;
    }

    private DecisionComputation buildDecisionReport(
            Candidate candidate,
            ParseReportResponse parseReport,
            List<DepartmentFeedback> feedbacks,
            List<InterviewPlan> interviews,
            List<InterviewEvaluation> evaluations
    ) {
        List<String> strengths = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> missingInformation = new ArrayList<>();
        List<String> supportingEvidence = new ArrayList<>();

        if (!parseReport.extractedSkills().isEmpty()) {
            strengths.add("简历识别出技能：" + String.join("、", parseReport.extractedSkills().stream().limit(5).toList()));
            supportingEvidence.add("简历技能标签：" + String.join("、", parseReport.extractedSkills().stream().limit(5).toList()));
        } else {
            missingInformation.add("简历技能标签较少，建议补充关键技术栈");
        }

        if (!parseReport.projectExperiences().isEmpty()) {
            strengths.add("已提取项目经历 " + parseReport.projectExperiences().size() + " 段");
        } else {
            risks.add("项目经历信息不足，难以判断项目深度");
        }

        if (!feedbacks.isEmpty()) {
            long passCount = feedbacks.stream().filter(feedback -> "PASS".equals(feedback.getDecision().name())).count();
            supportingEvidence.add("部门反馈 " + feedbacks.size() + " 条，其中通过 " + passCount + " 条");
            feedbacks.stream()
                    .map(DepartmentFeedback::getFeedback)
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst()
                    .ifPresent(item -> strengths.add("部门反馈认为：" + item));
            feedbacks.stream()
                    .map(DepartmentFeedback::getRejectReason)
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst()
                    .ifPresent(item -> risks.add("历史反馈中出现淘汰原因：" + item));
        } else {
            missingInformation.add("暂无部门反馈，建议先完成部门筛选");
        }

        if (!evaluations.isEmpty()) {
            double avgScore = evaluations.stream().mapToInt(InterviewEvaluation::getScore).average().orElse(0);
            supportingEvidence.add("已有 " + evaluations.size() + " 条面试评价，平均分 " + Math.round(avgScore));
            if (avgScore >= 80) {
                strengths.add("面试评价整体较好，平均分 " + Math.round(avgScore));
            } else if (avgScore < 65) {
                risks.add("面试平均分偏低，建议谨慎推进");
            }
            evaluations.stream()
                    .map(InterviewEvaluation::getStrengths)
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst()
                    .ifPresent(item -> strengths.add("面试亮点：" + item));
            evaluations.stream()
                    .map(InterviewEvaluation::getWeaknesses)
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst()
                    .ifPresent(item -> risks.add("面试风险：" + item));
        } else {
            missingInformation.add(interviews.isEmpty() ? "暂无面试安排" : "已有面试安排，但暂未收到面试评价");
        }

        parseReport.issues().stream()
                .filter(issue -> "WARN".equalsIgnoreCase(issue.severity()))
                .map(ParseIssueResponse::message)
                .forEach(missingInformation::add);

        int resumeScore = Math.min(100, parseReport.extractedSkills().size() * 15 + (parseReport.projectExperiences().isEmpty() ? 15 : 55));
        int feedbackScore = feedbacks.isEmpty() ? 40 : (int) Math.min(100, 50 + feedbacks.stream().filter(item -> "PASS".equals(item.getDecision().name())).count() * 20);
        int interviewScore;
        if (evaluations.isEmpty()) {
            interviewScore = interviews.isEmpty() ? 35 : 50;
        } else {
            interviewScore = (int) Math.round(evaluations.stream().mapToInt(InterviewEvaluation::getScore).average().orElse(0));
        }

        int recommendationScore = clamp((int) Math.round(resumeScore * 0.35 + feedbackScore * 0.25 + interviewScore * 0.40), 0, 100);
        String recommendationLevel = recommendationScore >= 85 ? "强烈推荐"
                : recommendationScore >= 70 ? "建议推进"
                : recommendationScore >= 55 ? "保守推进"
                : "暂缓";

        String recommendedAction = switch (candidate.getStatus()) {
            case NEW, TIMEOUT, IN_DEPT_REVIEW, PENDING_DEPT_REVIEW -> "先完成部门筛选，再决定是否安排面试";
            case PENDING_INTERVIEW -> "建议安排下一轮面试，补齐业务和稳定性判断";
            case INTERVIEWING -> "建议等待当前面试结果或补充终面评价";
            case INTERVIEW_PASSED -> "建议结合面试结果推进 Offer，或安排终面确认";
            case OFFER_PENDING -> "建议准备 Offer 方案并评估到岗风险";
            case OFFER_SENT -> "建议跟进候选人确认结果和入职时间";
            case HIRED -> "建议沉淀录用经验并进入入职跟进";
            case REJECTED -> "建议归档候选人并沉淀淘汰原因";
        };

        if (strengths.isEmpty()) {
            strengths.add("当前可用结构化信息较少，需要补齐更多资料再判断");
        }
        if (risks.isEmpty() && !missingInformation.isEmpty()) {
            risks.add("当前结论受限于资料完整度，建议补齐缺失项后再次分析");
        }

        String conclusion = "候选人当前处于「" + candidate.getStatus().getLabel() + "」，综合简历、反馈和面试信息，判断结果为「"
                + recommendationLevel + "」。";
        String reasoningSummary = "本次辅助决策综合参考了简历结构化信息、部门反馈、面试安排与面试评价。"
                + " 当前推荐动作为：" + recommendedAction + "。";

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("resumeReadiness", resumeScore);
        dimensionScores.put("feedbackConfidence", feedbackScore);
        dimensionScores.put("interviewConfidence", interviewScore);
        dimensionScores.put("overallRecommendation", recommendationScore);

        DecisionReportResponse report = new DecisionReportResponse(
                conclusion,
                recommendationScore,
                recommendationLevel,
                recommendedAction,
                strengths.stream().distinct().toList(),
                risks.stream().distinct().toList(),
                missingInformation.stream().distinct().toList(),
                supportingEvidence.stream().distinct().toList(),
                reasoningSummary,
                List.of(),
                List.of()
        );
        return new DecisionComputation(report, dimensionScores);
    }

    private ParseReportResponse buildFallbackParseReport(Map<String, Object> payload, ResumeAsset asset) throws IOException {
        if (asset == null) {
            return new ParseReportResponse(
                    "当前暂无简历附件，只能基于候选人已有资料进行辅助决策。",
                    List.of("未检测到简历附件"),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of(),
                    List.of(new ParseIssueResponse("WARN", "缺少简历附件，辅助决策依据不足")),
                    "NO_RESUME",
                    true
            );
        }
        return buildParseReport(payload, extractText(asset));
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
        return bytes.length >= 4
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F';
    }

    private String extractPdfText(byte[] bytes) {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException exception) {
            log.warn("Failed to extract pdf text with PDFBox", exception);
            return "";
        }
    }

    private String buildParseSummary(
            Map<String, ParseFieldValueResponse> fields,
            List<String> skills,
            List<ParseProjectResponse> projects,
            List<ParseIssueResponse> issues
    ) {
        StringBuilder builder = new StringBuilder("已完成简历结构化解析");
        if (!skills.isEmpty()) {
            builder.append("，识别 ").append(skills.size()).append(" 项技能");
        }
        if (!projects.isEmpty()) {
            builder.append("，提取 ").append(projects.size()).append(" 段项目描述");
        }
        if (fields.containsKey("phone") && fields.containsKey("email")) {
            builder.append("，联系方式完整");
        }
        if (!issues.isEmpty()) {
            builder.append("。仍有 ").append(issues.size()).append(" 项待确认信息");
        } else {
            builder.append("。当前结构化结果较完整");
        }
        return builder.toString();
    }

    private String buildParseDigest(ParseReportResponse parseReport) {
        List<String> parts = new ArrayList<>();
        if (!parseReport.highlights().isEmpty()) {
            parts.add("亮点：" + joinItems(parseReport.highlights()));
        }
        if (!parseReport.issues().isEmpty()) {
            parts.add("问题：" + joinItems(parseReport.issues().stream().map(ParseIssueResponse::message).toList()));
        }
        return joinItems(parts);
    }

    private String extractByPrefix(String text, String prefix) {
        for (String line : text.split("\n")) {
            if (line.startsWith(prefix + ":")) {
                return line.substring(prefix.length() + 1).trim();
            }
        }
        return null;
    }

    private String extractEducation(String text) {
        for (String keyword : EDUCATION_KEYWORDS) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private String extractLocation(String text) {
        for (String keyword : CITY_KEYWORDS) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private List<String> extractSkills(String text, String fallback) {
        Set<String> skills = new LinkedHashSet<>();
        for (String keyword : SKILL_KEYWORDS) {
            if (text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                skills.add(keyword);
            }
        }
        if (skills.isEmpty() && fallback != null) {
            skills.addAll(splitItems(fallback));
        }
        return new ArrayList<>(skills);
    }

    private List<ParseProjectResponse> extractProjects(String text, String fallback) {
        List<ParseProjectResponse> projects = Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.contains("项目") || line.contains("职责") || line.contains("负责"))
                .map(line -> new ParseProjectResponse("项目经历", line))
                .limit(3)
                .toList();
        if (!projects.isEmpty()) {
            return projects;
        }
        if (fallback == null || fallback.isBlank()) {
            return List.of();
        }
        return splitItems(fallback).stream()
                .map(item -> new ParseProjectResponse("项目经历", item))
                .toList();
    }

    private void captureField(Map<String, ParseFieldValueResponse> fields, String key, String value, double confidence, String source) {
        if (value == null || value.isBlank()) {
            return;
        }
        fields.put(key, new ParseFieldValueResponse(value, confidence, source));
    }

    private String fieldValue(ParseReportResponse parseReport, String key) {
        ParseFieldValueResponse field = parseReport.fields().get(key);
        return field == null ? null : field.value();
    }

    private void applyDraft(AgentResult result, ParsedCandidateDraftResponse draft) {
        result.setParsedName(draft.name());
        result.setParsedPhone(draft.phone());
        result.setParsedEmail(draft.email());
        result.setParsedLocation(draft.location());
        result.setParsedEducation(draft.education());
        result.setParsedExperience(draft.experience());
        result.setParsedSkillsSummary(draft.skillsSummary());
        result.setParsedProjectSummary(draft.projectSummary());
    }

    private Map<String, Object> readMap(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, new TypeReference<>() {
        });
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize mock agent result", exception);
        }
    }

    private String fileStem(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "待解析候选人";
        }
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String match(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1 == matcher.groupCount() ? 1 : 0) : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> splitItems(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,，;；\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String joinItems(List<String> items) {
        return items.stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .collect(Collectors.joining("；"));
    }

    private int averageScore(Map<String, Integer> scores) {
        if (scores.isEmpty()) {
            return 0;
        }
        return (int) Math.round(scores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void markFailed(AgentJob job, Exception exception) {
        log.warn("Failed to auto-complete job {}", job.getId(), exception);
        job.setStatus(AgentJobStatus.FAILED);
        job.setCompletedAt(OffsetDateTime.now());
        job.setLastError(exception.getMessage());
        agentJobRepository.save(job);
    }

    private record DecisionComputation(
            DecisionReportResponse report,
            Map<String, Integer> dimensionScores
    ) {
    }
}
