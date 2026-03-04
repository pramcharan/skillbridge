package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_token_hash",   columnList = "tokenHash", unique = true),
        @Index(name = "idx_token_expiry", columnList = "expiresAt")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Store a hash of the token, not the token itself
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant revokedAt;
}