package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reaction_msg_user_emoji",
                columnNames = {"message_id", "user_id", "emoji"}
        )
)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String emoji; // 👍 ❤️ 😂 🔥 💡

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}