// CommunityMessageRepository.java
package com.skillbridge.repository;

import com.skillbridge.entity.CommunityMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityMessageRepository
        extends JpaRepository<CommunityMessage, Long> {

    @Query("""
        SELECT m FROM CommunityMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.reactions r
        LEFT JOIN FETCH r.user
        WHERE m.room = :room
        ORDER BY m.createdAt DESC
        """)
    List<CommunityMessage> findRecentByRoom(
            @Param("room") String room, Pageable pageable);

    @Query("""
        SELECT m FROM CommunityMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.reactions r
        LEFT JOIN FETCH r.user
        WHERE m.room = :room AND m.pinned = true
        ORDER BY m.createdAt DESC
        """)
    List<CommunityMessage> findPinnedByRoom(@Param("room") String room);

    long countByRoom(String room);
}