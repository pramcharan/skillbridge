package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_messages",
        indexes = {
                @Index(name = "idx_cm_room",       columnList = "room"),
                @Index(name = "idx_cm_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_cm_sender",     columnList = "sender_id")
        })
@Data
public class CommunityMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String room;   // general | developers | creatives | opportunities | help

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean pinned     = false;
    private boolean isFile     = false;
    private String  fileUrl;
    private String  fileName;
    private String  fileType;

    // @mention usernames extracted: "alice,bob"
    @Column(length = 500)
    private String mentionedUsers;

    @OneToMany(mappedBy = "message",
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityReaction> reactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}