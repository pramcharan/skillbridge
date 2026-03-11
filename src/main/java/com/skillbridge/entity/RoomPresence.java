package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "room_presence",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "room"}))
@Data
public class RoomPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String room;

    @Column(nullable = false)
    private Instant lastSeen = Instant.now();

    private boolean online = true;
}