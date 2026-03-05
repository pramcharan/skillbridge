package com.skillbridge.service;

import com.skillbridge.dto.mapper.JobMapper;
import com.skillbridge.dto.request.PostJobRequest;
import com.skillbridge.dto.request.UpdateJobRequest;
import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.dto.response.JobDetailResponse;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.ProposalRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository      jobRepository;
    private final UserRepository     userRepository;
    private final ProposalRepository proposalRepository;
    private final JobMapper          jobMapper;

    // ── POST JOB ─────────────────────────────────────────────────────
    @Transactional
    public JobDetailResponse postJob(PostJobRequest request, String clientEmail) {
        User client = findUserByEmail(clientEmail);

        Job job = new Job();
        job.setClient(client);
        job.setTitle(request.getTitle().trim());
        job.setDescription(request.getDescription().trim());
        job.setCategory(request.getCategory());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setBudget(request.getBudget());
        job.setDeadline(request.getDeadline());
        job.setStatus(JobStatus.OPEN);
        job.setProposalCount(0);

        // Auto-expire in 30 days if not set
        if (request.getAutoExpireAt() != null) {
            job.setAutoExpireAt(request.getAutoExpireAt());
        } else {
            job.setAutoExpireAt(Instant.now().plus(30, ChronoUnit.DAYS));
        }

        Job saved = jobRepository.save(job);
        log.info("Job posted: '{}' by client: {}", saved.getTitle(), clientEmail);
        return jobMapper.toDetailResponse(saved);
    }

    // ── GET ALL JOBS (paginated + filtered) ───────────────────────────
    @Transactional(readOnly = true)
    public Page<JobCardResponse> getJobs(String keyword, JobCategory category,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> jobs;
        if (keyword != null && !keyword.isBlank()) {
            jobs = jobRepository.searchByKeyword(keyword.trim(), pageable);
        } else if (category != null) {
            jobs = jobRepository.findByStatusAndCategory(
                    JobStatus.OPEN, category, pageable);
        } else {
            jobs = jobRepository.findByStatus(JobStatus.OPEN, pageable);
        }

        return jobs.map(jobMapper::toCardResponse);
    }

    // ── GET JOB BY ID ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public JobDetailResponse getJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + jobId));
        return jobMapper.toDetailResponse(job);
    }

    // ── GET JOBS BY CLIENT ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<JobCardResponse> getClientJobs(String clientEmail,
                                               int page, int size) {
        User client = findUserByEmail(clientEmail);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobRepository.findByClient(client, pageable)
                .map(jobMapper::toCardResponse);
    }

    // ── UPDATE JOB ────────────────────────────────────────────────────
    @Transactional
    public JobDetailResponse updateJob(Long jobId, UpdateJobRequest request,
                                       String clientEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + jobId));

        // Only the job owner can edit
        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new AccessDeniedException("You can only edit your own jobs.");
        }

        if (request.getTitle()          != null) job.setTitle(request.getTitle());
        if (request.getDescription()    != null) job.setDescription(request.getDescription());
        if (request.getRequiredSkills() != null) job.setRequiredSkills(request.getRequiredSkills());
        if (request.getBudget()         != null) job.setBudget(request.getBudget());
        if (request.getStatus()         != null) job.setStatus(request.getStatus());

        return jobMapper.toDetailResponse(jobRepository.save(job));
    }

    // ── DELETE JOB ────────────────────────────────────────────────────
    @Transactional
    public void deleteJob(Long jobId, String clientEmail) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + jobId));

        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new AccessDeniedException("You can only delete your own jobs.");
        }

        jobRepository.delete(job);
        log.info("Job deleted: {} by {}", jobId, clientEmail);
    }

    // ── SIMILAR JOBS ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public java.util.List<JobCardResponse> getSimilarJobs(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        // Use first required skill to find similar
        String firstSkill = "";
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank()) {
            firstSkill = job.getRequiredSkills().split(",")[0].trim();
        }

        return jobRepository.findSimilarJobs(
                        job.getCategory(), firstSkill, jobId,
                        PageRequest.of(0, 3))
                .stream()
                .map(jobMapper::toCardResponse)
                .toList();
    }

    // ── AUTO EXPIRE (called by scheduler) ─────────────────────────────
    @Transactional
    public void expireOldJobs() {
        var expiredJobs = jobRepository.findByStatusAndAutoExpireAtBefore(
                JobStatus.OPEN, Instant.now());
        expiredJobs.forEach(job -> {
            job.setStatus(JobStatus.EXPIRED);
            log.info("Job auto-expired: {} ({})", job.getId(), job.getTitle());
        });
        jobRepository.saveAll(expiredJobs);
    }

    // ── HELPER ────────────────────────────────────────────────────────
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }
}