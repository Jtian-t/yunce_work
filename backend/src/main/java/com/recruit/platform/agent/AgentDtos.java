package com.recruit.platform.agent;

import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

record CreateAnalysisJobRequest(
        String jdSummary
) {
}

record CreateParseJobRequest(
        String hint
) {
}

record CreateDecisionJobRequest(
        String focusHint
) {
}

record AgentJobResponse(
        Long id,
        Long candidateId,
        AgentJobType jobType,
        AgentJobStatus status,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        AgentResultResponse result,
        String lastError
) {
}

record AgentResultResponse(
        String summary,
        Integer overallScore,
        Map<String, Integer> dimensionScores,
        String strengths,
        String risks,
        String recommendedAction,
        String rawReasoningDigest,
        ParsedCandidateDraftResponse parsedCandidateDraft,
        ParseReportResponse parseReport,
        DecisionReportResponse decisionReport
)
{
}

record ParseFieldValueResponse(
        String value,
        Double confidence,
        String source
) {
}

record ParseIssueResponse(
        String severity,
        String message
) {
}

record ParseProjectResponse(
        String title,
        String summary
) {
}

record ParseReportResponse(
        String summary,
        List<String> highlights,
        List<String> extractedSkills,
        List<ParseProjectResponse> projectExperiences,
        Map<String, ParseFieldValueResponse> fields,
        List<ParseIssueResponse> issues
) {
}

record DecisionReportResponse(
        String conclusion,
        Integer recommendationScore,
        String recommendationLevel,
        String recommendedAction,
        List<String> strengths,
        List<String> risks,
        List<String> missingInformation,
        List<String> supportingEvidence,
        String reasoningSummary
) {
}

record ParsedCandidateDraftResponse(
        String name,
        String phone,
        String email,
        String location,
        String education,
        String experience,
        String skillsSummary,
        String projectSummary
) {
}

record AgentCallbackRequest(
        @NotNull Boolean succeeded,
        String summary,
        Integer overallScore,
        Map<String, Integer> dimensionScores,
        String strengths,
        String risks,
        String recommendedAction,
        String rawReasoningDigest,
        String parsedName,
        String parsedPhone,
        String parsedEmail,
        String parsedLocation,
        String parsedEducation,
        String parsedExperience,
        String parsedSkillsSummary,
        String parsedProjectSummary,
        ParseReportResponse parseReport,
        DecisionReportResponse decisionReport,
        String errorMessage
) {
}
