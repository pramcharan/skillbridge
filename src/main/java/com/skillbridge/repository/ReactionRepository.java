package com.skillbridge.repository;

import com.skillbridge.entity.ChatMessage;
import com.skillbridge.entity.Reaction;
import com.skillbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByMessageAndUserAndEmoji(ChatMessage message, User user, String emoji);
    long countByMessageAndEmoji(ChatMessage message, String emoji);
}