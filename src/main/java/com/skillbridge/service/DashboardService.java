package com.skillbridge.service;

import com.skillbridge.dto.response.ClientDashboardStats;
import com.skillbridge.dto.response.FreelancerDashboardStats;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository         userRepository;
    private final JobRepository          jobRepository;
    private final ProposalRepository     proposalRepository;
    private final ProjectRepository      projectRepository;
    private final NotificationRepository notificationRepository;

    // ── FREELANCER STATS ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public FreelancerDashboardStats getFreelancerStats(String email) {
        User freelancer = findByEmail(email);
        Long userId     = freelancer.getId();

        // Proposal counts
        int total   = (int) proposalRepository.countByFreelancerId(userId);
        int pending = (int) proposalRepository
                .countByFreelancerIdAndStatus(userId, ProposalStatus.PENDING);

        // Project counts
        int active    = projectRepository
                .countByFreelancerIdAndStatus(userId, ProjectStatus.ACTIVE);
        int completed = projectRepository
                .countByFreelancerIdAndStatus(userId, ProjectStatus.COMPLETED);

        // Average AI match score across all proposals
        double avgScore = proposalRepository
                .findAverageMatchScoreByFreelancerId(userId)
                .orElse(0.0);

        return FreelancerDashboardStats.builder()
                .totalApplications(total)
                .pendingApplications(pending)
                .activeProjects(active)
                .completedProjects(completed)
                .avgMatchScore(Math.round(avgScore * 10.0) / 10.0)
                .avgRating(freelancer.getAvgRating())
                .reviewCount(freelancer.getReviewCount() != null
                        ? freelancer.getReviewCount() : 0)
                .profileCompletionPct(freelancer.getProfileCompletionPct() != null
                        ? freelancer.getProfileCompletionPct() : 0)
                .availabilityStatus(freelancer.getAvailabilityStatus() != null
                        ? freelancer.getAvailabilityStatus().name() : "UNKNOWN")
                .build();
    }

    // ── CLIENT STATS ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ClientDashboardStats getClientStats(String email) {
        User client = findByEmail(email);
        Long userId = client.getId();

        // Job counts
        int totalJobs  = (int) jobRepository.countByClientId(userId);
        int openJobs   = (int) jobRepository
                .countByClientIdAndStatus(userId, JobStatus.OPEN);
        int closedJobs = (int) jobRepository
                .countByClientIdAndStatus(userId, JobStatus.CLOSED);

        // Total proposals received on all client's jobs
        int totalProposals = proposalRepository
                .countProposalsForClient(userId);

        // Unread proposals
        int unreadProposals = proposalRepository
                .countUnreadProposalsForClient(userId);

        // Project counts
        int active    = projectRepository
                .countByClientIdAndStatus(userId, ProjectStatus.ACTIVE);
        int completed = projectRepository
                .countByClientIdAndStatus(userId, ProjectStatus.COMPLETED);

        return ClientDashboardStats.builder()
                .totalJobsPosted(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .totalProposalsReceived(totalProposals)
                .activeProjects(active)
                .completedProjects(completed)
                .unreadProposals(unreadProposals)
                .build();
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }
}