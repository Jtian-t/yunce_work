package com.recruit.platform.candidate;

import com.recruit.platform.common.enums.CandidateAdvanceAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;

record CandidateUpsertRequest(
        @NotBlank String name,
        @NotBlank String position,
        Long departmentId,
        Long ownerId,
        @NotBlank String source,
        @NotNull LocalDate submittedDate,
        String nextAction,
        String phone,
        String email,
        String location,
        String experience,
        String education,
        String skillsSummary,
        String projectSummary
) {
}

record CandidateResponse(
        Long id,
        String name,
        String position,
        String department,
        String statusCode,
        String statusLabel,
        String owner,
        String source,
        LocalDate submittedDate,
        OffsetDateTime updatedAt,
        String nextAction
) {
}

record CandidateDetailResponse(
        Long id,
        String name,
        String position,
        String department,
        String statusCode,
        String statusLabel,
        String owner,
        String source,
        LocalDate submittedDate,
        OffsetDateTime updatedAt,
        String nextAction,
        String phone,
        String email,
        String location,
        String experience,
        String education,
        String skillsSummary,
        String projectSummary,
        ResumeAssetResponse latestResume
) {
}

record ResumeAssetResponse(
        Long id,
        String originalFileName,
        String contentType,
        long fileSize,
        OffsetDateTime uploadedAt,
        String uploadedBy
) {
}

record TimelineEventResponse(
        Long id,
        String eventType,
        String actorName,
        String sourceAction,
        String statusCode,
        String statusLabel,
        String note,
        OffsetDateTime occurredAt
) {
}

record AdvanceCandidateRequest(
        @NotNull CandidateAdvanceAction action,
        Long departmentId,
        Long reviewerId,
        OffsetDateTime dueAt,
        Long interviewerId,
        String roundLabel,
        OffsetDateTime scheduledAt,
        OffsetDateTime endsAt,
        String note
) {
}
