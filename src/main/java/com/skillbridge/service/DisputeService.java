package com.skillbridge.service;

import com.skillbridge.dto.request.DisputeReplyRequest;
import com.skillbridge.dto.request.DisputeRequest;
import com.skillbridge.dto.request.DisputeResolveRequest;
import com.skillbridge.dto.response.DisputeResponse;
import com.skillbridge.entity.*;
import com.skillbridge.entity.enums.*;
import com.skillbridge.exception.*;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository   disputeRepository;
    private final ProjectRepository   projectRepository;
    private final UserRepository      userRepository;
    private final NotificationService notificationService;

    // ── Raise a dispute ────────────────────────────────────────────────
    @Transactional
    public DisputeResponse raiseDispute(String reporterEmail,
                                        DisputeRequest req) {
        User reporter = getUser(reporterEmail);

        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + req.getProjectId()));

        // Only project participants can raise disputes
        boolean isParticipant =
                project.getClient().getId().equals(reporter.getId()) ||
                        project.getFreelancer().getId().equals(reporter.getId());

        if (!isParticipant) {
            throw new BadRequestException(
                    "You are not a participant in this project.");
        }

        if (project.getStatus() == ProjectStatus.COMPLETED ||
                project.getStatus() == ProjectStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cannot raise a dispute for a completed or cancelled project.");
        }

        // Only one open dispute per project per user
        disputeRepository.findByProjectIdAndReporterId(
                        project.getId(), reporter.getId())
                .ifPresent(d -> {
                    if (d.getStatus() == DisputeStatus.OPEN ||
                            d.getStatus() == DisputeStatus.UNDER_REVIEW) {
                        throw new BadRequestException(
                                "You already have an open dispute for this project.");
                    }
                });

        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BadRequestException("Dispute reason is required.");
        }
        if (req.getDescription() == null ||
                req.getDescription().length() < 20) {
            throw new BadRequestException(
                    "Please provide a detailed description (min 20 chars).");
        }

        // Determine respondent (the other party)
        User respondent = project.getClient().getId()
                .equals(reporter.getId())
                ? project.getFreelancer()
                : project.getClient();

        DisputeTicket ticket = new DisputeTicket();
        ticket.setProject(project);
        ticket.setReporter(reporter);
        ticket.setRaisedBy(reporter);  // same person who raises the dispute
        ticket.setRespondent(respondent);
        ticket.setReason(req.getReason().trim());
        ticket.setDescription(req.getDescription().trim());
        ticket.setEvidenceUrls(req.getEvidenceUrls());
        ticket.setStatus(DisputeStatus.OPEN);

        DisputeTicket saved = disputeRepository.save(ticket);

        // Update project status to DISPUTED
        project.setStatus(ProjectStatus.DISPUTED);
        projectRepository.save(project);

        // Notify respondent
        notificationService.send(
                respondent,
                NotificationType.SYSTEM,
                "Dispute raised against you",
                reporter.getName() + " raised a dispute on project \"" +
                        project.getJob().getTitle() + "\": " + req.getReason(),
                "/disputes.html?id=" + saved.getId());

        // Notify admins (find any admin)
        userRepository.findFirstByRole(Role.ADMIN).ifPresent(admin ->
                notificationService.send(
                        admin,
                        NotificationType.SYSTEM,
                        "New dispute requires review",
                        "Project: " + project.getJob().getTitle() +
                                " — " + req.getReason(),
                        "/admin.html?tab=disputes"));

        log.info("Dispute raised: project={}, reporter={}",
                project.getId(), reporterEmail);

        return toResponse(saved);
    }

    // ── Respondent submits reply ───────────────────────────────────────
    @Transactional
    public DisputeResponse submitReply(Long disputeId,
                                       String respondentEmail,
                                       DisputeReplyRequest req) {
        DisputeTicket ticket = getTicket(disputeId);
        User user = getUser(respondentEmail);

        if (!ticket.getRespondent().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "Only the respondent can reply to this dispute.");
        }
        if (ticket.getStatus() == DisputeStatus.RESOLVED_COMPLETE ||
                ticket.getStatus() == DisputeStatus.CLOSED) {
            throw new BadRequestException(
                    "This dispute is already closed.");
        }

        ticket.setRespondentReply(req.getReply());
        ticket.setRespondentEvidenceUrls(req.getEvidenceUrls());
        ticket.setStatus(DisputeStatus.UNDER_REVIEW);

        DisputeTicket saved = disputeRepository.save(ticket);

        // Notify reporter
        notificationService.send(
                ticket.getReporter(),
                NotificationType.SYSTEM,
                "Dispute reply received",
                ticket.getRespondent().getName() +
                        " replied to your dispute. An admin will review shortly.",
                "/disputes.html?id=" + disputeId);

        return toResponse(saved);
    }

    // ── Admin resolves dispute ─────────────────────────────────────────
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId,
                                          String adminEmail,
                                          DisputeResolveRequest req) {
        User admin = getUser(adminEmail);
        if (!admin.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Only admins can resolve disputes.");
        }

        DisputeTicket ticket = getTicket(disputeId);
        if (ticket.getStatus() == DisputeStatus.RESOLVED_COMPLETE) {
            throw new BadRequestException("Dispute already resolved.");
        }

        DisputeResolution resolution =
                DisputeResolution.valueOf(req.getResolution());

        ticket.setStatus(DisputeStatus.RESOLVED_COMPLETE);
        ticket.setResolution(resolution);
        ticket.setAdminNotes(req.getAdminNotes());
        ticket.setResolvedBy(admin);
        ticket.setResolvedAt(Instant.now());

        DisputeTicket saved = disputeRepository.save(ticket);

        // Restore project status
        Project project = ticket.getProject();
        project.setStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);

        String resolutionText = switch (resolution) {
            case FAVOUR_REPORTER   -> "in favour of " + ticket.getReporter().getName();
            case FAVOUR_RESPONDENT -> "in favour of " + ticket.getRespondent().getName();
            case SPLIT             -> "split between both parties";
            case NO_ACTION         -> "with no action required";
        };

        // Notify both parties
        String msg = "Your dispute on \"" +
                ticket.getProject().getJob().getTitle() +
                "\" has been resolved " + resolutionText + ".";

        notificationService.send(ticket.getReporter(),
                NotificationType.SYSTEM, "Dispute resolved", msg,
                "/disputes.html?id=" + disputeId);

        notificationService.send(ticket.getRespondent(),
                NotificationType.SYSTEM, "Dispute resolved", msg,
                "/disputes.html?id=" + disputeId);

        log.info("Dispute {} resolved by {} as {}",
                disputeId, adminEmail, resolution);

        return toResponse(saved);
    }

    // ── Get my disputes ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DisputeResponse> getMyDisputes(String email) {
        User user = getUser(email);
        return disputeRepository.findByUserId(user.getId())
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get single dispute ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DisputeResponse getDispute(Long id, String email) {
        User user         = getUser(email);
        DisputeTicket t   = getTicket(id);

        boolean canView =
                t.getReporter().getId().equals(user.getId())   ||
                        t.getRespondent().getId().equals(user.getId()) ||
                        user.getRole().name().equals("ADMIN");

        if (!canView) throw new BadRequestException(
                "You do not have access to this dispute.");

        return toResponse(t);
    }

    // ── Admin — get all disputes ───────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DisputeResponse> getAllDisputes(int page, int size) {
        return disputeRepository.findAllForAdmin(
                        PageRequest.of(page, size))
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private DisputeTicket getTicket(Long id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispute not found: " + id));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private DisputeResponse toResponse(DisputeTicket t) {
        DisputeResponse r = new DisputeResponse();
        r.setId(t.getId());
        r.setProjectId(t.getProject().getId());
        r.setProjectTitle(t.getProject().getJob().getTitle());
        r.setReporterId(t.getReporter().getId());
        r.setReporterName(t.getReporter().getName());
        r.setRespondentId(t.getRespondent().getId());
        r.setRespondentName(t.getRespondent().getName());
        r.setReason(t.getReason());
        r.setDescription(t.getDescription());
        r.setRespondentReply(t.getRespondentReply());
        r.setAdminNotes(t.getAdminNotes());
        r.setStatus(t.getStatus().name());
        r.setResolution(t.getResolution() != null
                ? t.getResolution().name() : null);
        r.setResolvedByName(t.getResolvedBy() != null
                ? t.getResolvedBy().getName() : null);
        r.setCreatedAt(t.getCreatedAt());
        r.setResolvedAt(t.getResolvedAt());

        r.setEvidenceUrls(parseUrls(t.getEvidenceUrls()));
        r.setRespondentEvidenceUrls(
                parseUrls(t.getRespondentEvidenceUrls()));

        return r;
    }

    private List<String> parseUrls(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}