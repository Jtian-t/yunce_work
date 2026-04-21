package com.recruit.platform.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findTop200ByOrderByCreatedAtDesc();

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
}
