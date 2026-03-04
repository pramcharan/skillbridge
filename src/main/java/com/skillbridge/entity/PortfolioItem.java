package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "portfolio_items", indexes = {
        @Index(name = "idx_portfolio_user", columnList = "user_id")
})
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String projectUrl;
    private String imageUrl;

    // Comma-separated tags e.g. "React,Mobile,UI Design"
    private String tags;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}