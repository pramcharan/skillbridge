package com.skillbridge.service;

import com.skillbridge.dto.response.ProjectDetailResponse;
import com.skillbridge.dto.response.ProjectSummaryResponse;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.ChatMessageRepository;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository     projectRepository;
    private final UserRepository        userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService   notificationService;

    // ── GET ALL PROJECTS for a user ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getMyProjects(String email) {
        User user = findByEmail(email);

        // Check if client or freelancer
        List<Project> projects;
        boolean isClient = user.getRole() != null &&
                user.getRole().name().equals("CLIENT");

        if (isClient) {
            projects = projectRepository
                    .findByClientId(user.getId(), PageRequest.of(0, 50))
                    .getContent();
        } else {
            projects = projectRepository
                    .findByFreelancerId(user.getId(), PageRequest.of(0, 50))
                    .getContent();
        }

        return projects.stream()
                .map(p -> toSummary(p, user, isClient))
                .collect(Collectors.toList());
    }

    // ── GET SINGLE PROJECT DETAIL ─────────────────────────────────────
    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(Long projectId, String email) {
        User    user    = findByEmail(email);
        Project project = findProjectById(projectId);

        validateAccess(project, user);

        int unread = chatMessageRepository
                .countUnreadForUser(projectId, user.getId());

        return toDetail(project, unread);
    }

    // ── UPDATE PROJECT STATUS ─────────────────────────────────────────
    @Transactional
    public ProjectDetailResponse updateProjectStatus(Long projectId,
                                                     ProjectStatus newStatus,
                                                     String email) {
        User    user    = findByEmail(email);
        Project project = findProjectById(projectId);

        validateAccess(project, user);

        if (project.getStatus() == ProjectStatus.COMPLETED ||
                project.getStatus() == ProjectStatus.CANCELLED) {
            throw new com.skillbridge.exception.BadRequestException(
                    "Project is already " + project.getStatus().name().toLowerCase());
        }

        project.setStatus(newStatus);
        Project saved = projectRepository.save(project);

        // Notify the other party
        boolean isClient = user.getId().equals(project.getClient().getId());
        User otherParty = isClient ? project.getFreelancer() : project.getClient();
        String statusText = newStatus == ProjectStatus.COMPLETED ? "completed" : "cancelled";

        notificationService.send(
                otherParty,
                com.skillbridge.entity.enums.NotificationType.PROJECT_UPDATE,
                "Project " + statusText,
                "The project '" + project.getTitle() + "' has been marked as " + statusText + ".",
                "/project.html?id=" + projectId
        );

        log.info("Project {} marked as {} by {}", projectId, newStatus, email);
        return toDetail(saved, 0);
    }

    // ── MAPPERS ───────────────────────────────────────────────────────
    private ProjectSummaryResponse toSummary(Project p, User me, boolean iAmClient) {
        ProjectSummaryResponse r = new ProjectSummaryResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());

        // Other party
        User other = iAmClient ? p.getFreelancer() : p.getClient();
        if (other != null) {
            r.setOtherPartyName(other.getName());
            r.setOtherPartyAvatar(other.getAvatarUrl());
        }

        // Last message preview
        List<com.skillbridge.entity.ChatMessage> last =
                chatMessageRepository.findLastMessage(
                        p.getId(), PageRequest.of(0, 1));
        if (!last.isEmpty()) {
            String content = last.get(0).getContent();
            r.setLastMessage(content.length() > 60
                    ? content.substring(0, 60) + "…" : content);
            r.setLastMessageAt(last.get(0).getCreatedAt());
        }

        // Unread count
        r.setUnreadCount(chatMessageRepository
                .countUnreadForUser(p.getId(), me.getId()));

        return r;
    }

    private ProjectDetailResponse toDetail(Project p, int unread) {
        ProjectDetailResponse r = new ProjectDetailResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        r.setUnreadCount(unread);

        if (p.getClient() != null) {
            r.setClientId(p.getClient().getId());
            r.setClientName(p.getClient().getName());
            r.setClientAvatar(p.getClient().getAvatarUrl());
        }
        if (p.getFreelancer() != null) {
            r.setFreelancerId(p.getFreelancer().getId());
            r.setFreelancerName(p.getFreelancer().getName());
            r.setFreelancerAvatar(p.getFreelancer().getAvatarUrl());
        }
        if (p.getJob() != null) {
            r.setJobId(p.getJob().getId());
            r.setJobTitle(p.getJob().getTitle());
            r.setBudget(p.getJob().getBudget());
        }
        if (p.getProposal() != null) {
            r.setAcceptedRate(p.getProposal().getExpectedRate());
        }

        return r;
    }

    private void validateAccess(Project project, User user) {
        boolean isClient     = project.getClient().getId().equals(user.getId());
        boolean isFreelancer = project.getFreelancer().getId().equals(user.getId());
        if (!isClient && !isFreelancer) {
            throw new AccessDeniedException(
                    "You do not have access to this project.");
        }
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private Project findProjectById(Long id) {
        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
    }
}