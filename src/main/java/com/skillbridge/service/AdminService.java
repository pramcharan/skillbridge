package com.skillbridge.service;

import com.skillbridge.dto.response.*;
import com.skillbridge.entity.*;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final ChatMessageRepository chatMessageRepository;
    private final CommunityMessageRepository communityMessageRepository;
    private final CommunityReactionRepository communityReactionRepository;

    @Autowired
    @Lazy
    private final SimpMessagingTemplate messagingTemplate;


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
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        // 1. Delete associated projects first (they reference proposals)
        List<Project> projects = projectRepository.findByJobId(id);
        if (!projects.isEmpty()) {
            projectRepository.deleteAll(projects);
            log.info("Admin: deleted {} projects associated with job {}", projects.size(), id);
        }

        // 2. Now safe to delete the job (cascades to proposals and savedBy)
        jobRepository.delete(job);
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

        Double acceptedAvg = proposalRepository
                .avgScoreByStatus(ProposalStatus.ACCEPTED)
                .orElse(0.0);

        Double rejectedAvg = proposalRepository
                .avgScoreByStatus(ProposalStatus.REJECTED)
                .orElse(0.0);

        // Single row with 5 bucket columns
        List<String> distLabels = List.of("0-20", "21-40", "41-60", "61-80", "81-100");
        List<Long>   distData   = new ArrayList<>();
        try {
            Object[] distribution = proposalRepository.scoreDistribution();
            // Hibernate sometimes wraps single-row result in another Object[]
            if (distribution != null && distribution.length > 0
                    && distribution[0] instanceof Object[]) {
                distribution = (Object[]) distribution[0];
            }
            for (int i = 0; i < 5; i++) {
                if (distribution != null && i < distribution.length
                        && distribution[i] != null) {
                    distData.add(((Number) distribution[i]).longValue());
                } else {
                    distData.add(0L);
                }
            }
        } catch (Exception e) {
            log.warn("scoreDistribution failed: {}", e.getMessage());
            for (int i = 0; i < 5; i++) distData.add(0L);
        }

        // Top 5 jobs — returns (id, title, avgScore)
        List<Object[]> topJobs = proposalRepository
                .topJobsByAvgScore(PageRequest.of(0, 5));
        List<Map<String, Object>> topJobsList = new ArrayList<>();
        for (Object[] row : topJobs) {
            topJobsList.add(Map.of(
                    "jobTitle", row[1].toString(),                       // index 1 = title
                    "avgScore", Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0  // index 2 = avg
            ));
        }

        return Map.of(
                "acceptedAvgScore",  Math.round(acceptedAvg * 10.0) / 10.0,
                "rejectedAvgScore",  Math.round(rejectedAvg * 10.0) / 10.0,
                "scoreDistribution", Map.of("labels", distLabels, "data", distData),
                "topJobsByScore",    topJobsList
        );
    }


    // ── Project Chat ──────────────────────────────────────────────

    @Transactional
    public void deleteProjectChatMessage(Long messageId, Long projectId) {
        ChatMessage message = chatMessageRepository
                .findByIdAndProjectId(messageId, projectId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        chatMessageRepository.deleteByMessageId(messageId);

        // Notify connected clients to remove the message from UI
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MESSAGE_DELETED");
        payload.put("messageId", messageId);
        messagingTemplate.convertAndSend("/topic/chat." + projectId, (Object) payload);
    }

// ── Community Chat ────────────────────────────────────────────

    @Transactional
    public void deleteCommunityMessage(Long messageId, String room) {
        CommunityMessage message = communityMessageRepository
                .findByIdAndRoom(messageId, room)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Delete child reactions FIRST — otherwise FK constraint fails
        communityReactionRepository.deleteByMessageId(messageId);

        // Now safe to delete the parent message
        communityMessageRepository.deleteById(messageId);

        // Broadcast deletion to connected clients
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",      "MESSAGE_DELETED");
        payload.put("messageId", messageId);
        messagingTemplate.convertAndSend("/topic/community." + room, (Object) payload);
    }

// ── Fetch messages for admin review ──────────────────────────

    public List<Map<String, Object>> getProjectChatMessages(Long projectId, int page) {
        Pageable pageable = PageRequest.of(page, 50,
                Sort.by("createdAt").descending());
        return chatMessageRepository.findByProjectIdPaged(projectId, pageable)
                .stream()
                .map(m -> Map.<String, Object>of(
                        "id",        m.getId(),
                        "sender",    m.getSender().getName(),
                        "content",   m.getContent() != null ? m.getContent() : "",
                        "isFile",    m.isFile(),
                        "fileName",  m.getFileName() != null ? m.getFileName() : "",
                        "createdAt", m.getCreatedAt().toString()
                ))
                .toList();
    }

    public List<Map<String, Object>> getCommunityRoomMessages(String room, int page) {
        Pageable pageable = PageRequest.of(page, 50,
                Sort.by("createdAt").descending());
        return communityMessageRepository
                .findRecentByRoom(room, pageable)
                .stream()
                .map(m -> Map.<String, Object>of(
                        "id",        m.getId(),
                        "sender",    m.getSender().getName(),
                        "content",   m.getContent() != null ? m.getContent() : "",
                        "isFile",    m.isFile(),
                        "fileName",  m.getFileName() != null ? m.getFileName() : "",
                        "room",      m.getRoom(),
                        "createdAt", m.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> getPlatformActivity() {
        List<AdminActivityResponse> activities = new ArrayList<>();
        Pageable top5 = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        
        userRepository.findAllForAdmin(top5).forEach(u -> 
            activities.add(new AdminActivityResponse(
                "<strong>" + u.getName() + "</strong> registered as " + u.getRole(),
                formatTimeAgo(u.getCreatedAt()), "teal", u.getCreatedAt()
            ))
        );
        
        jobRepository.findAllForAdmin(top5).forEach(j ->
            activities.add(new AdminActivityResponse(
                "<strong>" + j.getClient().getName() + "</strong> posted Job — " + j.getTitle(),
                formatTimeAgo(j.getCreatedAt()), "gold", j.getCreatedAt()
            ))
        );
        
        proposalRepository.findAll(top5).forEach(p ->
            activities.add(new AdminActivityResponse(
                "New proposal submitted on <strong>" + p.getJob().getTitle() + "</strong>",
                formatTimeAgo(p.getCreatedAt()), "purple", p.getCreatedAt()
            ))
        );

        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return activities.stream().limit(5).toList();
    }

    private String formatTimeAgo(Instant instant) {
        if (instant == null) return "Unknown";
        long seconds = java.time.Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) return seconds + " seconds ago";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        return (seconds / 86400) + " days ago";
    }
}