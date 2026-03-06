package com.skillbridge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FreelancerDashboardStats {
    private int    totalApplications;
    private int    pendingApplications;
    private int    activeProjects;
    private int    completedProjects;
    private double avgMatchScore;
    private Double avgRating;
    private int    reviewCount;
    private int    profileCompletionPct;
    private String availabilityStatus;
}