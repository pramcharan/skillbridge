package com.skillbridge.service;

import com.skillbridge.entity.Notification;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // REQUIRES_NEW = completely separate transaction
    // If this fails it does NOT roll back the caller's transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User recipient, NotificationType type,
                     String title, String message, String link) {
        try {
            Notification n = new Notification();
            n.setUser(recipient);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setLink(link);
            n.setIsRead(false);
            notificationRepository.save(n);
            log.info("Notification sent to {}: {}", recipient.getEmail(), title);
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}",
                    recipient.getEmail(), e.getMessage());
        }
    }
}