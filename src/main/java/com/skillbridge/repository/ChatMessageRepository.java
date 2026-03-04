package com.skillbridge.repository;

import com.skillbridge.entity.ChatMessage;
import com.skillbridge.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);
    Optional<ChatMessage> findFirstByRoomIdAndIsPinnedTrue(String roomId);
}