package com.skillbridge.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientDashboardStats {
    private int totalJobsPosted;
    private int openJobs;
    private int closedJobs;
    private int totalProposalsReceived;
    private int activeProjects;
    private int completedProjects;
    private int unreadProposals;
}