package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_project", columnList = "project_id"),
        @Index(name = "idx_chat_room",    columnList = "roomId"),
        @Index(name = "idx_chat_room_time", columnList = "roomId, createdAt")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null for community chat messages
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Room ID for community chat: "general", "developers", etc.
    // Null for project messages
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Comma-separated user IDs mentioned with @
    private String mentionedUserIds;

    // File attachment URL (Cloudinary)
    private String fileUrl;
    private String fileName;
    private String fileType;     // image/png, application/pdf etc
    private boolean isFile = false;

    @Column(nullable = false)
    private Boolean isPinned = false;

    @Column(nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reaction> reactions;
}