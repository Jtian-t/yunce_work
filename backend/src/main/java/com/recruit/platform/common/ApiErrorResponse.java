package com.recruit.platform.common;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors,
        OffsetDateTime timestamp
) {
}
