// RoomPresenceRepository.java
package com.skillbridge.repository;

import com.skillbridge.entity.RoomPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoomPresenceRepository
        extends JpaRepository<RoomPresence, Long> {

    Optional<RoomPresence> findByUserIdAndRoom(Long userId, String room);

    @Query("""
        SELECT rp FROM RoomPresence rp
        JOIN FETCH rp.user
        WHERE rp.room = :room AND rp.online = true
        AND rp.lastSeen > :since
        """)
    List<RoomPresence> findOnlineInRoom(
            @Param("room")  String room,
            @Param("since") Instant since);

    @Modifying
    @Query("UPDATE RoomPresence rp SET rp.online = false WHERE rp.lastSeen < :cutoff")
    void markStaleOffline(@Param("cutoff") Instant cutoff);
}