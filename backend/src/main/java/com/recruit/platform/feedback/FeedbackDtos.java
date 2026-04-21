package com.recruit.platform.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

record SubmitFeedbackRequest(
        @NotNull Long assignmentId,
        @NotNull com.recruit.platform.common.enums.FeedbackDecision decision,
        @NotBlank String feedback,
        String rejectReason,
        String nextStep,
        String suggestedInterviewer
) {
}

record FeedbackResponse(
        Long id,
        Long candidateId,
        Long assignmentId,
        String reviewer,
        String decision,
        String feedback,
        String rejectReason,
        String nextStep,
        String suggestedInterviewer,
        OffsetDateTime createdAt
) {
}
