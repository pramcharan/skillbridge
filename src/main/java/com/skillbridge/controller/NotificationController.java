package com.skillbridge.controller;

import com.skillbridge.dto.response.NotificationResponse;
import com.skillbridge.dto.response.UnreadCountResponse;
import com.skillbridge.entity.Notification;
import com.skillbridge.entity.User;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.NotificationRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    // ── GET paginated notifications ───────────────────────────────────
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = findUser(email);
        return ResponseEntity.ok(
                notificationRepository
                        .findByUserId(user.getId(), PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    // ── GET unread count (for bell icon) ──────────────────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal String email) {
        User user  = findUser(email);
        int  count = notificationRepository
                .countByUserIdAndIsRead(user.getId(), false);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    // ── MARK single notification as read ─────────────────────────────
    @Transactional
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        notificationRepository.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    // ── MARK ALL as read ──────────────────────────────────────────────
    @Transactional
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal String email) {
        User user = findUser(email);
        notificationRepository.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ── DELETE single notification ────────────────────────────────────
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        notificationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // ── Mapper ────────────────────────────────────────────────────────
    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setLink(n.getLink());
        r.setIsRead(n.getIsRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }
}