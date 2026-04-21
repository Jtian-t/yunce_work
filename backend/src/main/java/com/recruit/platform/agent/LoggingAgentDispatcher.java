package com.recruit.platform.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.candidate.ResumeAsset;
import com.recruit.platform.candidate.ResumeAssetRepository;
import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import com.recruit.platform.storage.ResumeStorageService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAgentDispatcher implements AgentDispatcher {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("1\\d{10}");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+\\s*年)");

    private final AgentJobRepository agentJobRepository;
    private final AgentResultRepository agentResultRepository;
    private final ResumeAssetRepository resumeAssetRepository;
    private final ResumeStorageService resumeStorageService;
    private final ObjectMapper objectMapper;

    @Override
    public void dispatch(AgentJob job, String payloadJson) {
        log.info("Dispatching agent job {} with payload {}", job.getId(), payloadJson);
        if (job.getJobType() == AgentJobType.PARSE) {
            autoCompleteParseJob(job, payloadJson);
        }
    }

    @Transactional
    void autoCompleteParseJob(AgentJob job, String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            ResumeAsset asset = resumeAssetRepository.findTopByCandidateIdOrderByUploadedAtDesc(job.getCandidate().getId()).orElse(null);
            String resumeText = asset == null ? "" : readText(asset);
            ParsedFields parsedFields = parseFields(payload, resumeText);

            job.setStatus(AgentJobStatus.SUCCEEDED);
            job.setStartedAt(OffsetDateTime.now());
            job.setCompletedAt(OffsetDateTime.now());
            agentJobRepository.save(job);

            AgentResult result = agentResultRepository.findByJobId(job.getId()).orElseGet(AgentResult::new);
            result.setJob(job);
            result.setSummary("Resume fields parsed for review");
            result.setOverallScore(0);
            result.setDimensionScoresJson("{}");
            result.setRecommendedAction("Review parsed draft");
            result.setRawReasoningDigest("Local mock parser completed");
            result.setParsedName(parsedFields.name());
            result.setParsedPhone(parsedFields.phone());
            result.setParsedEmail(parsedFields.email());
            result.setParsedEducation(parsedFields.education());
            result.setParsedExperience(parsedFields.experience());
            result.setParsedSkillsSummary(parsedFields.skillsSummary());
            result.setParsedProjectSummary(parsedFields.projectSummary());
            agentResultRepository.save(result);
        } catch (Exception exception) {
            log.warn("Failed to auto-complete parse job {}", job.getId(), exception);
            job.setStatus(AgentJobStatus.FAILED);
            job.setCompletedAt(OffsetDateTime.now());
            job.setLastError(exception.getMessage());
            agentJobRepository.save(job);
        }
    }

    private ParsedFields parseFields(Map<String, Object> payload, String text) {
        String normalized = text == null ? "" : text.replace('\r', '\n');
        String fileName = stringValue(payload.get("originalFileName"));
        String fallbackName = stringValue(payload.get("fallbackName"));
        String fallbackPhone = stringValue(payload.get("fallbackPhone"));
        String fallbackEmail = stringValue(payload.get("fallbackEmail"));
        String fallbackEducation = stringValue(payload.get("fallbackEducation"));
        String fallbackExperience = stringValue(payload.get("fallbackExperience"));
        String fallbackSkills = stringValue(payload.get("fallbackSkillsSummary"));
        String fallbackProject = stringValue(payload.get("fallbackProjectSummary"));

        String name = firstNonBlank(extractByPrefix(normalized, "姓名"), extractByPrefix(normalized, "Name"), fallbackName, fileStem(fileName));
        String phone = firstNonBlank(match(normalized, PHONE_PATTERN), fallbackPhone);
        String email = firstNonBlank(match(normalized, EMAIL_PATTERN), fallbackEmail);
        String education = firstNonBlank(extractEducation(normalized), fallbackEducation);
        String experience = firstNonBlank(match(normalized, EXPERIENCE_PATTERN), fallbackExperience);
        String skillsSummary = firstNonBlank(extractSkills(normalized), fallbackSkills);
        String projectSummary = firstNonBlank(extractProjects(normalized), fallbackProject, "待补充项目经历");

        return new ParsedFields(
                name,
                phone,
                email,
                education,
                experience,
                skillsSummary,
                projectSummary
        );
    }

    private String readText(ResumeAsset asset) throws IOException {
        try (InputStream inputStream = resumeStorageService.openStream(asset.getObjectKey())) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractEducation(String text) {
        for (String keyword : new String[]{"博士", "硕士", "本科", "大专"}) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private String extractSkills(String text) {
        Set<String> skills = new LinkedHashSet<>();
        for (String keyword : new String[]{"Java", "Spring Boot", "Spring", "React", "Vue", "MySQL", "Redis", "Kafka", "Docker"}) {
            if (text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                skills.add(keyword);
            }
        }
        return skills.isEmpty() ? null : String.join(", ", skills);
    }

    private String extractProjects(String text) {
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.contains("项目") || trimmed.contains("经历") || trimmed.contains("负责")) {
                return trimmed;
            }
        }
        return null;
    }

    private String extractByPrefix(String text, String prefix) {
        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix + "：") || trimmed.startsWith(prefix + ":")) {
                int index = trimmed.indexOf(':');
                if (index < 0) {
                    index = trimmed.indexOf('：');
                }
                if (index >= 0 && index + 1 < trimmed.length()) {
                    return trimmed.substring(index + 1).trim();
                }
            }
        }
        return null;
    }

    private String match(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1 == matcher.groupCount() ? 1 : 0) : null;
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ParsedFields(
            String name,
            String phone,
            String email,
            String education,
            String experience,
            String skillsSummary,
            String projectSummary
    ) {
    }
}
