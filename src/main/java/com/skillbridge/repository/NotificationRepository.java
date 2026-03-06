package com.skillbridge.repository;

import com.skillbridge.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(
            @Param("userId") Long userId, Pageable pageable);

    // Unread count — for bell icon badge
    int countByUserIdAndIsRead(Long userId, Boolean isRead);

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    // Mark single as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    // Cleanup old read notifications (for scheduler)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true " +
            "AND n.createdAt < :cutoff")
    void deleteOldReadNotifications(@Param("cutoff") Instant cutoff);

    // Recent unread — for real-time push
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
            Long userId);
}