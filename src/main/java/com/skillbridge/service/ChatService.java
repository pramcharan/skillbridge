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

    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository     projectRepository;
    private final UserRepository        userRepository;
    private final NotificationService   notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ── GET chat history ──────────────────────────────────────────────
    @Transactional
    public List<ChatMessageResponse> getChatHistory(Long projectId, String email) {
        User    user    = findByEmail(email);
        Project project = findProject(projectId);
        validateAccess(project, user);

        chatMessageRepository.markAllAsRead(projectId, user.getId());

        return chatMessageRepository.findByProjectId(projectId)
                .stream()
                .map(m -> toResponse(m, user.getId()))
                .collect(Collectors.toList());
    }

    // ── SEND message via REST ─────────────────────────────────────────
    // FIX 1: added replyToId parameter — replaces the broken "request" reference
    @Transactional
    public ChatMessageResponse sendMessage(Long projectId,
                                           String content,
                                           String senderEmail,
                                           Long replyToId) {
        User    sender  = findByEmail(senderEmail);
        Project project = findProject(projectId);
        validateAccess(project, sender);

        ChatMessage message = new ChatMessage();
        message.setProject(project);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setIsRead(false);

        // FIX 1: use the replyToId parameter directly, not "request.getReplyTo()"
        if (replyToId != null) {
            chatMessageRepository.findById(replyToId)
                    .ifPresent(message::setReplyTo);
        }

        ChatMessage saved = chatMessageRepository.save(message);

        project.setLastMessageAt(saved.getCreatedAt());
        projectRepository.save(project);

        ChatMessageResponse response = toResponse(saved, sender.getId());

        messagingTemplate.convertAndSend(
                "/topic/project." + projectId,
                response);

        User recipient = sender.getId().equals(project.getClient().getId())
                ? project.getFreelancer()
                : project.getClient();

        notificationService.send(
                recipient,
                NotificationType.NEW_MESSAGE,
                "New message from " + sender.getName(),
                content.length() > 80 ? content.substring(0, 80) + "…" : content,
                "/project.html?id=" + projectId
        );

        log.debug("Message sent in project {} by {}", projectId, senderEmail);
        return response;
    }

    // Overload for callers that don't pass replyToId (backward compatibility)
    @Transactional
    public ChatMessageResponse sendMessage(Long projectId,
                                           String content,
                                           String senderEmail) {
        return sendMessage(projectId, content, senderEmail, null);
    }

    // ── WebSocket message handler ─────────────────────────────────────
    @Transactional
    public void handleWebSocketMessage(Long projectId,
                                       String content,
                                       String senderEmail,
                                       Long replyToId) {
        try {
            sendMessage(projectId, content, senderEmail, replyToId);
        } catch (Exception e) {
            log.error("WebSocket message handling failed: {}", e.getMessage());
        }
    }

    // Overload for backward compatibility
    @Transactional
    public void handleWebSocketMessage(Long projectId,
                                       String content,
                                       String senderEmail) {
        handleWebSocketMessage(projectId, content, senderEmail, null);
    }

    // ── Mapper ────────────────────────────────────────────────────────
    // FIX 2 + FIX 3: renamed "msg" → "m" and "response" → "r"
    // to match the actual parameter and local variable names
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

        // FIX 2 + FIX 3: use "m" and "r" not "msg" and "response"
        if (m.getReplyTo() != null) {
            r.setReplyToId(m.getReplyTo().getId());
            try {
                String replyContent = m.getReplyTo().getContent();
                r.setReplyToContent(
                        replyContent != null && replyContent.length() > 80
                                ? replyContent.substring(0, 80) + "…"
                                : replyContent
                );
                r.setReplyToSender(
                        m.getReplyTo().getSender() != null
                                ? m.getReplyTo().getSender().getName()
                                : "Unknown"
                );
            } catch (Exception e) {
                // replyTo lazy proxy failed — skip silently
            }
        }

        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void validateAccess(Project project, User user) {
        boolean isClient     = project.getClient().getId().equals(user.getId());
        boolean isFreelancer = project.getFreelancer().getId().equals(user.getId());
        if (!isClient && !isFreelancer) {
            throw new AccessDeniedException(
                    "You don't have access to this project.");
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