package com.skillbridge.entity;

import com.skillbridge.entity.enums.AvailabilityStatus;
import com.skillbridge.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email",  columnList = "email",  unique = true),
        @Index(name = "idx_user_role",   columnList = "role"),
        @Index(name = "idx_user_active", columnList = "isActive")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Nullable — OAuth users have no password
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String bio;
    private String avatarUrl;

    // Stored as comma-separated string e.g. "React,Spring Boot,MySQL"
    @Column(columnDefinition = "TEXT")
    private String skills;

    private Double hourlyRate;

    @Column(nullable = false)
    private Double avgRating = 0.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false)
    private Integer profileCompletionPct = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    // OAuth fields
    private String googleId;
    private String githubId;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isEmailVerified = false;

    private Instant lastActive;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // Relationships (mapped by other side)
    @OneToMany(mappedBy = "client",     cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Job> postedJobs;

    @OneToMany(mappedBy = "freelancer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Proposal> proposals;

    @OneToMany(mappedBy = "user",       cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PortfolioItem> portfolio;
}