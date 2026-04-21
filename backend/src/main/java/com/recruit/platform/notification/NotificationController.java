package com.recruit.platform.notification;

import com.recruit.platform.common.enums.NotificationType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    List<NotificationResponse> listMine(
            @RequestParam(defaultValue = "my") String scope,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) NotificationType type
    ) {
        return notificationService.listMine(scope, departmentId, userId, type);
    }

    @PostMapping("/api/notifications/{id}/read")
    NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
