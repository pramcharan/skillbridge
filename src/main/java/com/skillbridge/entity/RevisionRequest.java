package com.skillbridge.entity;

import com.skillbridge.entity.enums.RevisionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "revision_requests", indexes = {
        @Index(name = "idx_revision_project", columnList = "project_id"),
        @Index(name = "idx_revision_requester", columnList = "requester_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RevisionStatus status;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @CreationTimestamp
    private Instant createdAt;

    private Instant resolvedAt;
}