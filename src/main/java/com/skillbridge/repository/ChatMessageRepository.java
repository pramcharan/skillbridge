package com.skillbridge.repository;

import com.skillbridge.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // All messages for a project — oldest first
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.project.id = :projectId " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessage> findByProjectId(@Param("projectId") Long projectId);

    // Unread count for a user in a project
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.project.id = :projectId " +
            "AND m.sender.id != :userId " +
            "AND m.isRead = false")
    int countUnreadForUser(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    // Mark all messages as read for a user
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
            "WHERE m.project.id = :projectId " +
            "AND m.sender.id != :userId " +
            "AND m.isRead = false")
    void markAllAsRead(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    // Last message in a project (for project list preview)
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.project.id = :projectId " +
            "ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastMessage(
            @Param("projectId") Long projectId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT MAX(m.createdAt) FROM ChatMessage m WHERE m.project.id = :projectId")
    Optional<Instant> findLastMessageTime(@Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.id = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);

    Optional<ChatMessage> findByIdAndProjectId(Long id, Long projectId);

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender " +
            "WHERE m.project.id = :projectId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByProjectIdPaged(@Param("projectId") Long projectId,
                                           Pageable pageable);
}