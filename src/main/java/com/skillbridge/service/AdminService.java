package com.skillbridge.service;

import com.skillbridge.dto.response.*;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}