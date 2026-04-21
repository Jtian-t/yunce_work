package com.recruit.platform.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.security.CurrentUserService;
import com.recruit.platform.user.User;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void create(User recipient, NotificationType type, String title, String content) {
        create(recipient, type, title, content, Map.of());
    }

    @Transactional
    public void create(User recipient, NotificationType type, String title, String content, Map<String, Object> payload) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setPayloadJson(write(payload));
        notificationRepository.save(notification);
    }

    public List<NotificationResponse> listMine() {
        Long recipientId = currentUserService.getRequiredUser().getId();
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Long recipientId = currentUserService.getRequiredUser().getId();
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt(),
                readPayload(notification.getPayloadJson())
        );
    }

    private String write(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize notification payload", exception);
        }
    }

    private Map<String, Object> readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }
}
