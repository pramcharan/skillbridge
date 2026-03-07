package com.skillbridge.service;

import com.skillbridge.dto.response.ChatMessageResponse;
import com.skillbridge.entity.ChatMessage;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.ChatMessageRepository;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository  chatMessageRepository;
    private final ProjectRepository      projectRepository;
    private final UserRepository         userRepository;
    private final NotificationService    notificationService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ── GET chat history ──────────────────────────────────────────────
    @Transactional
    public List<ChatMessageResponse> getChatHistory(Long projectId, String email) {
        User    user    = findByEmail(email);
        Project project = findProject(projectId);
        validateAccess(project, user);

        // Mark messages as read
        chatMessageRepository.markAllAsRead(projectId, user.getId());

        return chatMessageRepository.findByProjectId(projectId)
                .stream()
                .map(m -> toResponse(m, user.getId()))
                .collect(Collectors.toList());
    }

    // ── SEND message via REST (fallback if WebSocket fails) ───────────
    @Transactional
    public ChatMessageResponse sendMessage(Long projectId,
                                           String content,
                                           String senderEmail) {
        User    sender  = findByEmail(senderEmail);
        Project project = findProject(projectId);
        validateAccess(project, sender);

        ChatMessage message = new ChatMessage();
        message.setProject(project);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setIsRead(false);

        ChatMessage saved = chatMessageRepository.save(message);

        // Update project lastMessageAt
        project.setLastMessageAt(saved.getCreatedAt());
        projectRepository.save(project);

        ChatMessageResponse response = toResponse(saved, sender.getId());

        // Push via WebSocket to project room
        messagingTemplate.convertAndSend(
                "/topic/project." + projectId,
                response);

        // Notify the other party
        User recipient = sender.getId().equals(project.getClient().getId())
                ? project.getFreelancer()
                : project.getClient();

        notificationService.send(
                recipient,
                NotificationType.NEW_MESSAGE,
                "New message from " + sender.getName(),
                content.length() > 80 ? content.substring(0,80)+"…" : content,
                "/project.html?id=" + projectId
        );

        log.debug("Message sent in project {} by {}", projectId, senderEmail);
        return response;
    }

    // ── WebSocket message handler ─────────────────────────────────────
    @Transactional
    public void handleWebSocketMessage(Long projectId,
                                       String content,
                                       String senderEmail) {
        try {
            sendMessage(projectId, content, senderEmail);
        } catch (Exception e) {
            log.error("WebSocket message handling failed: {}", e.getMessage());
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────
    private ChatMessageResponse toResponse(ChatMessage m, Long currentUserId) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.setId(m.getId());
        r.setProjectId(m.getProject().getId());
        r.setContent(m.getContent());
        r.setIsRead(m.getIsRead());
        r.setCreatedAt(m.getCreatedAt());
        r.setMine(m.getSender().getId().equals(currentUserId));

        if (m.getSender() != null) {
            r.setSenderId(m.getSender().getId());
            r.setSenderName(m.getSender().getName());
            r.setSenderAvatar(m.getSender().getAvatarUrl());
        }
        return r;
    }

    private void validateAccess(Project project, User user) {
        boolean isClient     = project.getClient().getId().equals(user.getId());
        boolean isFreelancer = project.getFreelancer().getId().equals(user.getId());
        if (!isClient && !isFreelancer) {
            throw new AccessDeniedException("You don't have access to this project.");
        }
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private Project findProject(Long id) {
        return projectRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
    }
}