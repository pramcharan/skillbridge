package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_reset_token", columnList = "token", unique = true),
        @Index(name = "idx_reset_user",  columnList = "user_id")
})
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}