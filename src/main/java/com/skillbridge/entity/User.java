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

    // ── Onboarding flag ──────────────────────────────────────────────
    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete = false;

    // ── Profile fields (may already exist — add only if missing) ─────
    @Column(name = "location")
    private String location;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "experience_level", length = 30)
    private String experienceLevel;    // ENTRY | INTERMEDIATE | EXPERT

    @Column(name = "availability", length = 30)
    private String availability;       // FULL_TIME | PART_TIME | FLEXIBLE

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "hiring_goal", length = 30)
    private String hiringGoal;         // ONE_TIME | ONGOING | TEAM | EXPLORE

    @Column(name = "budget_range", length = 20)
    private String budgetRange;        // MICRO | SMALL | MEDIUM | LARGE | ENTERPRISE

}