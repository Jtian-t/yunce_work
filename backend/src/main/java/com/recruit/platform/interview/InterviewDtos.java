package com.recruit.platform.interview;

import com.recruit.platform.common.enums.InterviewResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

record CreateInterviewPlanRequest(
        @NotNull Long candidateId,
        @NotNull Long interviewerId,
        @NotBlank String roundLabel,
        @NotNull OffsetDateTime scheduledAt,
        @NotNull OffsetDateTime endsAt
) {
}

record SubmitInterviewEvaluationRequest(
        @NotNull InterviewResult result,
        @NotNull @Min(0) @Max(100) Integer score,
        @NotBlank String evaluation,
        String strengths,
        String weaknesses,
        String suggestion
) {
}

record InterviewPlanResponse(
        Long id,
        Long candidateId,
        String roundLabel,
        String interviewer,
        String status,
        OffsetDateTime scheduledAt,
        OffsetDateTime endsAt,
        List<InterviewEvaluationResponse> evaluations
) {
}

record InterviewEvaluationResponse(
        Long id,
        String interviewer,
        String result,
        Integer score,
        String evaluation,
        String strengths,
        String weaknesses,
        String suggestion,
        OffsetDateTime createdAt
) {
}
