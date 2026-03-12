package com.skillbridge.entity;

import com.skillbridge.entity.enums.DisputeResolution;
import com.skillbridge.entity.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "dispute_tickets",
        indexes = {
                @Index(name = "idx_dispute_project",  columnList = "project_id"),
                @Index(name = "idx_dispute_status",   columnList = "status"),
                @Index(name = "idx_dispute_reporter", columnList = "reporter_id")
        })
@Data
public class DisputeTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;   // who raised the dispute

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id", nullable = false)
    private User respondent; // the other party

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy; // admin who resolved

    @Column(nullable = false, length = 100)
    private String reason;   // short reason title

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidenceUrls;  // comma-separated Cloudinary URLs

    @Column(columnDefinition = "TEXT")
    private String respondentReply; // other party's response

    @Column(columnDefinition = "TEXT")
    private String respondentEvidenceUrls;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;    // admin resolution notes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private DisputeResolution resolution; // FAVOUR_REPORTER, FAVOUR_RESPONDENT, SPLIT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant resolvedAt;
}