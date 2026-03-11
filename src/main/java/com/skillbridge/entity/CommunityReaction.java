package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "community_reactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"message_id", "user_id", "emoji"}))
@Data
public class CommunityReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private CommunityMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String emoji;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}