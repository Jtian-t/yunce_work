package com.recruit.platform.dashboard;

public record AlertResponse(
        Long assignmentId,
        Long candidateId,
        String candidateName,
        String position,
        String department,
        long overdueHours
) {
}
