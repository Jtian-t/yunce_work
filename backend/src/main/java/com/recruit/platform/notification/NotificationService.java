package com.recruit.platform.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.platform.common.ForbiddenException;
import com.recruit.platform.common.NotFoundException;
import com.recruit.platform.common.enums.NotificationType;
import com.recruit.platform.common.enums.RoleType;
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

    public List<NotificationResponse> listMine(String scope, Long departmentId, Long userId, NotificationType type) {
        User actor = currentUserService.getRequiredUser();
        currentUserService.requireAnyRole(RoleType.INTERVIEWER, RoleType.DEPARTMENT_LEAD, RoleType.HR, RoleType.ADMIN);
        String normalizedScope = "department".equalsIgnoreCase(scope) ? "department" : "my";
        List<Notification> source = "department".equals(normalizedScope)
                ? notificationRepository.findTop200ByOrderByCreatedAtDesc()
                : notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(actor.getId());

        return source.stream()
                .filter(notification -> matchesScope(notification, actor, normalizedScope, departmentId, userId))
                .filter(notification -> type == null || notification.getType() == type)
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

    private boolean matchesScope(Notification notification, User actor, String scope, Long departmentId, Long userId) {
        if (!"department".equals(scope)) {
            return notification.getRecipient().getId().equals(actor.getId());
        }

        if (currentUserService.hasAnyRole(RoleType.INTERVIEWER)
                && !currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN, RoleType.DEPARTMENT_LEAD)) {
            return notification.getRecipient().getId().equals(actor.getId());
        }

        Map<String, Object> payload = readPayload(notification.getPayloadJson());
        Long payloadDepartmentId = readLong(payload.get("departmentId"));
        Long payloadInterviewerId = readLong(payload.get("interviewerId"));

        if (currentUserService.hasAnyRole(RoleType.DEPARTMENT_LEAD)
                && !currentUserService.hasAnyRole(RoleType.HR, RoleType.ADMIN)) {
            Long actorDepartmentId = actor.getDepartment() == null ? null : actor.getDepartment().getId();
            if (actorDepartmentId == null) {
                throw new ForbiddenException("Current user is not bound to a department");
            }
            if (!actorDepartmentId.equals(payloadDepartmentId)) {
                return false;
            }
            return userId == null || userId.equals(payloadInterviewerId) || userId.equals(notification.getRecipient().getId());
        }

        if (departmentId != null && !departmentId.equals(payloadDepartmentId)) {
            return false;
        }
        if (userId != null && !userId.equals(payloadInterviewerId) && !userId.equals(notification.getRecipient().getId())) {
            return false;
        }
        return true;
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
