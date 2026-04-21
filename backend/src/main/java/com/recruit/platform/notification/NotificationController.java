package com.recruit.platform.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    List<NotificationResponse> listMine() {
        return notificationService.listMine();
    }

    @PostMapping("/api/notifications/{id}/read")
    NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
