package com.skillbridge.service;

import com.skillbridge.dto.response.*;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository     userRepository;
    private final JobRepository      jobRepository;
    private final ProposalRepository proposalRepository;
    private final ProjectRepository  projectRepository;
    private final ReviewRepository   reviewRepository;

    // ── PLATFORM STATS ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminStatsResponse getPlatformStats() {
        AdminStatsResponse s = new AdminStatsResponse();

        // Users
        s.setTotalUsers(userRepository.count());
        s.setTotalFreelancers(userRepository.countByRole(Role.FREELANCER));
        s.setTotalClients(userRepository.countByRole(Role.CLIENT));
        s.setActiveUsers(userRepository.countByIsActiveTrue());

        // Jobs
        s.setTotalJobs(jobRepository.count());
        s.setOpenJobs(jobRepository.countByStatus(JobStatus.OPEN));
        s.setClosedJobs(jobRepository.countByStatus(JobStatus.CLOSED));

        // Proposals
        s.setTotalProposals(proposalRepository.count());

        // Projects
        s.setTotalProjects(projectRepository.count());
        s.setActiveProjects(projectRepository.countByStatus(ProjectStatus.ACTIVE));
        s.setCompletedProjects(projectRepository.countByStatus(ProjectStatus.COMPLETED));

        // Reviews
        s.setTotalReviews(reviewRepository.count());
        s.setAvgPlatformRating(
                reviewRepository.findPlatformAverageRating()
                        .map(d -> Math.round(d * 10.0) / 10.0)
                        .orElse(0.0));

        return s;
    }

    // ── LIST USERS ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String query,
                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> users = (query != null && !query.isBlank())
                ? userRepository.searchUsers(query, pageable)
                : userRepository.findAllForAdmin(pageable);

        return users.map(this::toAdminUserResponse);
    }

    // ── GET SINGLE USER ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + id));
        return toAdminUserResponse(user);
    }

    // ── DEACTIVATE / REACTIVATE USER ──────────────────────────────────
    @Transactional
    public AdminUserResponse setUserActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + id));
        user.setIsActive(active);
        User saved = userRepository.save(user);
        log.info("Admin: user {} set active={}", id, active);
        return toAdminUserResponse(saved);
    }

    // ── PROMOTE USER TO ADMIN ─────────────────────────────────────────
    @Transactional
    public AdminUserResponse promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + id));
        user.setRole(Role.ADMIN);
        User saved = userRepository.save(user);
        log.info("Admin: user {} promoted to ADMIN", id);
        return toAdminUserResponse(saved);
    }

    // ── LIST JOBS ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminJobResponse> getJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobRepository.findAllForAdmin(pageable)
                .map(this::toAdminJobResponse);
    }

    // ── FORCE CLOSE JOB ───────────────────────────────────────────────
    @Transactional
    public AdminJobResponse forceCloseJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + id));
        job.setStatus(JobStatus.CLOSED);
        Job saved = jobRepository.save(job);
        log.info("Admin: job {} force closed", id);
        return toAdminJobResponse(saved);
    }

    // ── DELETE JOB ────────────────────────────────────────────────────
    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found: " + id);
        }
        jobRepository.deleteById(id);
        log.info("Admin: job {} deleted", id);
    }

    // ── MAPPERS ───────────────────────────────────────────────────────
    private AdminUserResponse toAdminUserResponse(User u) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole() != null ? u.getRole().name() : null);
        r.setIsActive(u.getIsActive());
        r.setIsEmailVerified(u.getIsEmailVerified());
        r.setProfileCompletionPct(u.getProfileCompletionPct());
        r.setAvgRating(u.getAvgRating());
        r.setReviewCount(u.getReviewCount());
        r.setCreatedAt(u.getCreatedAt());
        r.setLastActive(u.getLastActive());
        try {
            if (u.getRole() == Role.CLIENT) {
                r.setTotalJobs((int) jobRepository.countByClientId(u.getId()));
            } else {
                r.setTotalProposals(
                        (int) proposalRepository.countByFreelancerId(u.getId()));
            }
        } catch (Exception ignored) {}
        return r;
    }

    private AdminJobResponse toAdminJobResponse(Job j) {
        AdminJobResponse r = new AdminJobResponse();
        r.setId(j.getId());
        r.setTitle(j.getTitle());
        r.setCategory(j.getCategory() != null ? j.getCategory().name() : null);
        r.setStatus(j.getStatus() != null ? j.getStatus().name() : null);
        r.setBudget(j.getBudget());
        r.setProposalCount(j.getProposalCount() != null
                ? j.getProposalCount() : 0);
        r.setCreatedAt(j.getCreatedAt());
        r.setDeadline(j.getDeadline());
        if (j.getClient() != null) {
            r.setClientName(j.getClient().getName());
            r.setClientEmail(j.getClient().getEmail());
        }
        return r;
    }

    public Map<String, Object> getChartData() {
        Map<String, Object> charts = new HashMap<>();

        charts.put("userRegistrations",  getUserRegistrationsLast7Days());
        charts.put("jobsByCategory",     getJobsByCategory());
        charts.put("proposalsByStatus",  getProposalsByStatus());
        charts.put("revenueByWeek",      getProjectsCompletedLast8Weeks());

        return charts;
    }

    private Map<String, Object> getUserRegistrationsLast7Days() {
        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date  = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            Instant   start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant   end   = date.plusDays(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            long count = userRepository.countByCreatedAtBetween(start, end);
            labels.add(date.getDayOfWeek().name().substring(0,3));
            data.add(count);
        }
        return Map.of("labels", labels, "data", data);
    }

    private Map<String, Object> getJobsByCategory() {
        List<Object[]> rows = jobRepository.countGroupByCategory();
        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0].toString());
            data.add(((Number) row[1]).longValue());
        }
        return Map.of("labels", labels, "data", data);
    }

    private Map<String, Object> getProposalsByStatus() {
        List<Object[]> rows = proposalRepository.countGroupByStatus();
        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0].toString());
            data.add(((Number) row[1]).longValue());
        }
        return Map.of("labels", labels, "data", data);
    }

    private Map<String, Object> getProjectsCompletedLast8Weeks() {
        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = LocalDate.now(ZoneOffset.UTC)
                    .minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            Instant start = weekStart.atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            Instant end   = weekStart.plusWeeks(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            long count = projectRepository
                    .countByStatusAndCreatedAtBetween(
                            ProjectStatus.COMPLETED, start, end);
            labels.add("W" + (8 - i));
            data.add(count);
        }
        return Map.of("labels", labels, "data", data);
    }

    public Map<String, Object> getAiHealthData() {
        // Avg AI score for ACCEPTED proposals
        Double acceptedAvg = proposalRepository
                .avgScoreByStatus(ProposalStatus.ACCEPTED)
                .orElse(0.0);

        // Avg AI score for REJECTED proposals
        Double rejectedAvg = proposalRepository
                .avgScoreByStatus(ProposalStatus.REJECTED)
                .orElse(0.0);

        // Score distribution buckets: 0-20, 21-40, 41-60, 61-80, 81-100
        List<Object[]> distribution = proposalRepository.scoreDistribution();
        List<String>   distLabels   = new ArrayList<>();
        List<Long>     distData     = new ArrayList<>();
        for (Object[] row : distribution) {
            distLabels.add(row[0].toString());
            distData.add(((Number) row[1]).longValue());
        }

        // Top 5 jobs by avg proposal score
        List<Object[]> topJobs = proposalRepository.topJobsByAvgScore();
        List<Map<String,Object>> topJobsList = new ArrayList<>();
        for (Object[] row : topJobs) {
            topJobsList.add(Map.of(
                    "jobTitle", row[0].toString(),
                    "avgScore", ((Number) row[1]).doubleValue()
            ));
        }

        return Map.of(
                "acceptedAvgScore",  Math.round(acceptedAvg * 10.0) / 10.0,
                "rejectedAvgScore",  Math.round(rejectedAvg * 10.0) / 10.0,
                "scoreDistribution", Map.of("labels", distLabels, "data", distData),
                "topJobsByScore",    topJobsList
        );
    }
}