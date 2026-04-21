package com.recruit.platform.workflow;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

record AssignmentRequest(
        @NotNull Long departmentId,
        @NotNull Long reviewerId,
        OffsetDateTime dueAt
) {
}

record AssignmentResponse(
        Long id,
        Long candidateId,
        String candidateName,
        String department,
        String reviewer,
        String status,
        OffsetDateTime dueAt,
        OffsetDateTime completedAt,
        OffsetDateTime remindedAt
) {
}
