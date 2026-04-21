package com.recruit.platform.notification;

import com.recruit.platform.common.enums.NotificationType;
import java.time.OffsetDateTime;
import java.util.Map;

record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        boolean read,
        OffsetDateTime createdAt,
        Map<String, Object> payload
) {
}
