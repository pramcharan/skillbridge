package com.skillbridge.entity;

import com.skillbridge.entity.enums.ProposalStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "proposals", indexes = {
        @Index(name = "idx_proposal_job",        columnList = "job_id"),
        @Index(name = "idx_proposal_freelancer", columnList = "freelancer_id"),
        @Index(name = "idx_proposal_status",     columnList = "status")
})
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String coverLetter;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    private Double expectedRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.PENDING;

    // AI scoring fields
    private Double aiMatchScore;

    @Column(columnDefinition = "TEXT")
    private String aiMatchReason;

    private String aiMatchBadge; // GREEN, AMBER, RED

    @Column(nullable = false)
    private Boolean viewedByClient = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}