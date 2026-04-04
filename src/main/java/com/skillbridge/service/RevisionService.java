package com.skillbridge.service;

import com.skillbridge.dto.request.RevisionRequestDTO;
import com.skillbridge.dto.request.RevisionStatusUpdateDTO;
import com.skillbridge.dto.response.RevisionResponse;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.RevisionRequest;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.RevisionStatus;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.RevisionRequestRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionRequestRepository revisionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final int MAX_OPEN_REVISIONS = 10;

    // ── Create ────────────────────────────────────────────────

    @Transactional
    public RevisionResponse createRevision(Long projectId,
                                           RevisionRequestDTO dto,
                                           String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isClient     = project.getClient().getId().equals(requester.getId());
        boolean isFreelancer = project.getFreelancer() != null &&
                project.getFreelancer().getId().equals(requester.getId());

        if (!isClient && !isFreelancer)
            throw new RuntimeException("Not a project participant");

        if (project.getStatus() != ProjectStatus.ACTIVE)
            throw new RuntimeException("Revisions can only be raised on active projects");

        long openCount = revisionRepository
                .countByProjectIdAndStatus(projectId, RevisionStatus.OPEN);
        if (openCount >= MAX_OPEN_REVISIONS)
            throw new RuntimeException("Too many open revisions on this project");

        RevisionRequest revision = RevisionRequest.builder()
                .project(project)
                .requester(requester)
                .title(dto.title())
                .description(dto.description())
                .status(RevisionStatus.OPEN)
                .build();

        revisionRepository.save(revision);

        // Notify the other participant
        User recipient = isClient ? project.getFreelancer() : project.getClient();
        if (recipient != null) {
            notificationService.send(
                    recipient,
                    NotificationType.GENERAL,
                    "New Revision Request",
                    requester.getName() + " raised a revision on \"" + project.getTitle() + "\": " + dto.title(),
                    "/project.html?id=" + projectId
            );
        }

        return toResponse(revision);
    }

    // ── Update Status ─────────────────────────────────────────

    @Transactional
    public RevisionResponse updateStatus(Long revisionId,
                                         RevisionStatusUpdateDTO dto,
                                         String updaterEmail) {
        RevisionRequest revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new RuntimeException("Revision not found"));

        User updater = userRepository.findByEmail(updaterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = revision.getProject();

        boolean isClient     = project.getClient().getId().equals(updater.getId());
        boolean isFreelancer = project.getFreelancer() != null &&
                project.getFreelancer().getId().equals(updater.getId());

        if (!isClient && !isFreelancer)
            throw new RuntimeException("Not authorised");

        // Only the other participant (not the requester) can resolve/reject
        if (revision.getRequester().getId().equals(updater.getId()) &&
                dto.status() != RevisionStatus.IN_PROGRESS)
            throw new RuntimeException("You cannot resolve your own revision request");

        revision.setStatus(dto.status());

        if (dto.resolutionNote() != null && !dto.resolutionNote().isBlank())
            revision.setResolutionNote(dto.resolutionNote());

        if (dto.status() == RevisionStatus.RESOLVED ||
                dto.status() == RevisionStatus.REJECTED) {
            revision.setResolvedBy(updater);
            revision.setResolvedAt(Instant.now());
        }

        revisionRepository.save(revision);

        // Notify requester
        String statusLabel = dto.status().name().toLowerCase().replace("_", " ");
        notificationService.send(
                revision.getRequester(),
                NotificationType.GENERAL,
                "Revision " + dto.status().name().charAt(0) +
                        dto.status().name().substring(1).toLowerCase(),
                "Your revision \"" + revision.getTitle() + "\" has been marked as " + statusLabel,
                "/project.html?id=" + project.getId()
        );

        return toResponse(revision);
    }

    // ── Queries ───────────────────────────────────────────────

    public List<RevisionResponse> getProjectRevisions(Long projectId,
                                                      String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isParticipant =
                project.getClient().getId().equals(user.getId()) ||
                        (project.getFreelancer() != null &&
                                project.getFreelancer().getId().equals(user.getId()));

        if (!isParticipant)
            throw new RuntimeException("Not authorised");

        return revisionRepository.findByProjectId(projectId)
                .stream().map(this::toResponse).toList();
    }

    // ── Mapper ────────────────────────────────────────────────

    private RevisionResponse toResponse(RevisionRequest r) {
        Project project = r.getProject();
        return new RevisionResponse(
                r.getId(),
                project.getId(),
                project.getTitle(),
                r.getRequester().getName(),
                r.getRequester().getAvatarUrl(),
                project.getClient().getId().equals(r.getRequester().getId()),
                r.getTitle(),
                r.getDescription(),
                r.getStatus(),
                r.getResolutionNote(),
                r.getResolvedBy() != null ? r.getResolvedBy().getName() : null,
                r.getCreatedAt(),
                r.getResolvedAt()
        );
    }
}