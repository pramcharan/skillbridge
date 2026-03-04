package com.skillbridge.repository;

import com.skillbridge.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RevokedToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}