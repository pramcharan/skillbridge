package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "saved_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_saved_job_user",
                columnNames = {"user_id", "job_id"}
        )
)
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant savedAt;
}