// CommunityReactionRepository.java
package com.skillbridge.repository;

import com.skillbridge.entity.CommunityReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityReactionRepository
        extends JpaRepository<CommunityReaction, Long> {

    Optional<CommunityReaction> findByMessageIdAndUserIdAndEmoji(
            Long messageId, Long userId, String emoji);

    @Modifying
    @Query("DELETE FROM CommunityReaction r WHERE r.message.id = :msgId AND r.user.id = :userId AND r.emoji = :emoji")
    void deleteByMessageIdAndUserIdAndEmoji(
            @Param("msgId")   Long msgId,
            @Param("userId")  Long userId,
            @Param("emoji")   String emoji);

    @Query("SELECT COUNT(r) FROM CommunityReaction r WHERE r.message.id = :msgId AND r.emoji = :emoji")
    long countByMessageIdAndEmoji(
            @Param("msgId")  Long msgId,
            @Param("emoji")  String emoji);
}