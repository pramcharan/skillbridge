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

    @Column(name = "avatar_url")
    private String avatarUrl;

    // Stored as comma-separated string e.g. "React,Spring Boot,MySQL"
    @Column(columnDefinition = "TEXT")
    private String skills;

    private Double hourlyRate;

    @Column(nullable = false, name = "avg_rating")
    private Double avgRating = 0.0;

    @Column(nullable = false, name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "profile_completion_pct")
    private Integer profileCompletionPct = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    // OAuth fields
    private String googleId;
    private String githubId;

    // OAuth account linking (for users who registered with email/password)
    private String linkedGoogle;   // linked Google email
    private String linkedGithub;   // linked GitHub email/login

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

    private String location;
    private String portfolioUrl;
}