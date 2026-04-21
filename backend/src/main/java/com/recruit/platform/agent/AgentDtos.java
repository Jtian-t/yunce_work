package com.recruit.platform.agent;

import com.recruit.platform.common.enums.AgentJobStatus;
import com.recruit.platform.common.enums.AgentJobType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;

record CreateAnalysisJobRequest(
        String jdSummary
) {
}

record CreateParseJobRequest(
        String hint
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
        ParsedCandidateDraftResponse parsedCandidateDraft
)
{
}

record ParsedCandidateDraftResponse(
        String name,
        String phone,
        String email,
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
        String parsedEducation,
        String parsedExperience,
        String parsedSkillsSummary,
        String parsedProjectSummary,
        String errorMessage
) {
}
