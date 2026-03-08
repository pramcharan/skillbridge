package com.skillbridge.dto.response;

import lombok.Data;

@Data
public class AdminStatsResponse {
    // Users
    private long totalUsers;
    private long totalFreelancers;
    private long totalClients;
    private long activeUsers;

    // Jobs
    private long totalJobs;
    private long openJobs;
    private long closedJobs;

    // Proposals
    private long totalProposals;

    // Projects
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;

    // Reviews
    private long  totalReviews;
    private double avgPlatformRating;
}