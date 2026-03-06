package com.skillbridge.service;

import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.dto.mapper.ProposalMapper;
import com.skillbridge.dto.request.SubmitProposalRequest;
import com.skillbridge.dto.request.UpdateProposalStatusRequest;
import com.skillbridge.dto.response.ProposalResponse;
import com.skillbridge.dto.response.ProposalSummaryResponse;
import com.skillbridge.entity.*;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import com.skillbridge.service.ai.AiScoringOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository    proposalRepository;
    private final JobRepository         jobRepository;
    private final UserRepository        userRepository;
    private final ProjectRepository     projectRepository;
    private final NotificationService notificationService;
    private final ProposalMapper        proposalMapper;
    private final AiScoringOrchestrator aiScoringOrchestrator;

    // ── SUBMIT PROPOSAL ───────────────────────────────────────────────
    @Transactional
    public ProposalResponse submitProposal(SubmitProposalRequest request,
                                           String freelancerEmail) {
        User freelancer = findUserByEmail(freelancerEmail);
        Job  job        = findJobById(request.getJobId());

        // Validations
        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job is no longer accepting proposals.");
        }

        if (proposalRepository.existsByJobIdAndFreelancerId(
                job.getId(), freelancer.getId())) {
            throw new BadRequestException(
                    "You have already applied to this job.");
        }

        if (job.getClient().getId().equals(freelancer.getId())) {
            throw new BadRequestException(
                    "You cannot apply to your own job.");
        }

        // Calculate AI score instantly
        AiMatchResult score = aiScoringOrchestrator.scoreSync(freelancer, job);

        // Build proposal
        Proposal proposal = new Proposal();
        proposal.setJob(job);
        proposal.setFreelancer(freelancer);
        proposal.setCoverLetter(request.getCoverLetter());
        proposal.setExpectedRate(request.getExpectedRate());
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setViewedByClient(false);
        proposal.setAiMatchScore(score.getFinalScore());
        proposal.setAiMatchBadge(score.getBadge());
        proposal.setAiMatchReason(score.getExplanation());

        Proposal saved = proposalRepository.save(proposal);

        // Increment proposal count on job
        job.setProposalCount(job.getProposalCount() + 1);
        jobRepository.save(job);

        // Notify client
        sendNotification(
                job.getClient(),
                NotificationType.NEW_PROPOSAL,
                "New proposal received",
                freelancer.getName() + " applied to your job: " + job.getTitle(),
                "/proposals-client.html?jobId=" + job.getId()
        );

        // Async AI enrichment — updates the proposal explanation in background
        // ── Schedule async AI enrichment AFTER transaction commits ────────
        Long proposalId    = saved.getId();
        User freelancerRef = freelancer;
        Job  jobRef        = job;

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        aiScoringOrchestrator.scoreAsync(freelancerRef, jobRef,
                                enrichedResult -> {
                                    proposalRepository.findById(proposalId).ifPresent(p -> {
                                        p.setAiMatchReason(enrichedResult.getExplanation());
                                        p.setAiMatchBadge(enrichedResult.getBadge());
                                        proposalRepository.save(p);
                                        log.info("Proposal {} AI explanation enriched by {}",
                                                proposalId, enrichedResult.getProvider());
                                    });
                                });
                    }
                }
        );

        log.info("Proposal submitted: job={} freelancer={} score={}",
                job.getId(), freelancerEmail, score.getFinalScore());

        return proposalMapper.toResponse(saved);
    }

    // ── GET PROPOSALS FOR A JOB (client view) ─────────────────────────
    @Transactional
    public List<ProposalResponse> getProposalsForJob(Long jobId,
                                                     String clientEmail) {
        Job job = findJobById(jobId);

        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new AccessDeniedException(
                    "You can only view proposals for your own jobs.");
        }

        List<Proposal> proposals =
                proposalRepository.findByJobIdOrderByAiMatchScoreDesc(jobId);

        // Mark all as viewed
        proposals.forEach(p -> {
            if (!p.getViewedByClient()) {
                p.setViewedByClient(true);
            }
        });
        proposalRepository.saveAll(proposals);

        return proposals.stream()
                .map(proposalMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── GET SINGLE PROPOSAL ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProposalResponse getProposalById(Long proposalId, String userEmail) {
        Proposal proposal = proposalRepository.findByIdWithDetails(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proposal not found: " + proposalId));

        // Only the freelancer who applied or the job's client can view
        boolean isFreelancer = proposal.getFreelancer()
                .getEmail().equals(userEmail);
        boolean isClient     = proposal.getJob().getClient()
                .getEmail().equals(userEmail);

        if (!isFreelancer && !isClient) {
            throw new AccessDeniedException(
                    "You do not have permission to view this proposal.");
        }

        return proposalMapper.toResponse(proposal);
    }

    // ── GET MY PROPOSALS (freelancer view) ────────────────────────────
    @Transactional(readOnly = true)
    public Page<ProposalSummaryResponse> getMyProposals(String freelancerEmail,
                                                        int page, int size) {
        User freelancer = findUserByEmail(freelancerEmail);
        Pageable pageable = PageRequest.of(page, size);

        return proposalRepository
                .findByFreelancerId(freelancer.getId(), pageable)
                .map(proposalMapper::toSummary);
    }

    // ── UPDATE PROPOSAL STATUS (client accepts or rejects) ────────────
    @Transactional
    public ProposalResponse updateProposalStatus(Long proposalId,
                                                 UpdateProposalStatusRequest request,
                                                 String clientEmail) {
        Proposal proposal = proposalRepository.findByIdWithDetails(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proposal not found: " + proposalId));

        Job job = proposal.getJob();

        if (!job.getClient().getEmail().equals(clientEmail)) {
            throw new AccessDeniedException(
                    "You can only manage proposals for your own jobs.");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException(
                    "This proposal has already been " +
                            proposal.getStatus().name().toLowerCase() + ".");
        }

        ProposalStatus newStatus = request.getStatus();

        if (newStatus == ProposalStatus.ACCEPTED) {
            handleAcceptance(proposal, job);
        } else if (newStatus == ProposalStatus.REJECTED) {
            handleRejection(proposal);
        } else {
            throw new BadRequestException(
                    "Invalid status. Use ACCEPTED or REJECTED.");
        }

        Proposal saved = proposalRepository.save(proposal);
        log.info("Proposal {} {} by client {}",
                proposalId, newStatus, clientEmail);

        return proposalMapper.toResponse(saved);
    }

    // ── WITHDRAW PROPOSAL (freelancer withdraws before response) ──────
    @Transactional
    public void withdrawProposal(Long proposalId, String freelancerEmail) {
        Proposal proposal = proposalRepository.findByIdWithDetails(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proposal not found: " + proposalId));

        if (!proposal.getFreelancer().getEmail().equals(freelancerEmail)) {
            throw new AccessDeniedException(
                    "You can only withdraw your own proposals.");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException(
                    "You can only withdraw pending proposals.");
        }

        proposal.setStatus(ProposalStatus.WITHDRAWN);
        proposalRepository.save(proposal);

        // Decrement proposal count
        Job job = proposal.getJob();
        job.setProposalCount(Math.max(0, job.getProposalCount() - 1));
        jobRepository.save(job);

        log.info("Proposal {} withdrawn by {}", proposalId, freelancerEmail);
    }

    // ── ACCEPT HELPER — creates project + notifies + closes job ──────
    private void handleAcceptance(Proposal proposal, Job job) {
        proposal.setStatus(ProposalStatus.ACCEPTED);

        // Close job so no more proposals
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

        // Reject all other pending proposals for this job
        List<Proposal> otherProposals =
                proposalRepository.findByJobIdAndStatus(
                        job.getId(), ProposalStatus.PENDING);
        otherProposals.forEach(p -> {
            if (!p.getId().equals(proposal.getId())) {
                p.setStatus(ProposalStatus.REJECTED);
                // Notify rejected freelancers
                sendNotification(
                        p.getFreelancer(),
                        NotificationType.PROPOSAL_UPDATE,
                        "Application update",
                        "Unfortunately your proposal for '" +
                                job.getTitle() + "' was not selected.",
                        "/dashboard-freelancer.html"
                );
            }
        });
        proposalRepository.saveAll(otherProposals);

        // Create the project
        Project project = new Project();
        project.setClient(job.getClient());
        project.setFreelancer(proposal.getFreelancer());
        project.setProposal(proposal);
        project.setJob(job);
        project.setTitle(job.getTitle());
        project.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(project);

        // Notify the accepted freelancer
        sendNotification(
                proposal.getFreelancer(),
                NotificationType.PROPOSAL_UPDATE,
                "🎉 Proposal accepted!",
                "Your proposal for '" + job.getTitle() +
                        "' was accepted! The project is now active.",
                "/dashboard-freelancer.html"
        );

        log.info("Project created for job={} freelancer={}",
                job.getId(), proposal.getFreelancer().getEmail());
    }

    // ── REJECT HELPER ─────────────────────────────────────────────────
    private void handleRejection(Proposal proposal) {
        proposal.setStatus(ProposalStatus.REJECTED);

        sendNotification(
                proposal.getFreelancer(),
                NotificationType.PROPOSAL_UPDATE,
                "Application update",
                "Your proposal for '" + proposal.getJob().getTitle() +
                        "' was not selected this time.",
                "/dashboard-freelancer.html"
        );
    }

    // ── NOTIFICATION HELPER ───────────────────────────────────────────
    private void sendNotification(User recipient, NotificationType type,
                                  String title, String message, String link) {
        notificationService.send(recipient, type, title, message, link);
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private Job findJobById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));
    }
}