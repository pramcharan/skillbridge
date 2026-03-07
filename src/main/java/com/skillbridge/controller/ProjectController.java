package com.skillbridge.controller;

import com.skillbridge.dto.request.SendMessageRequest;
import com.skillbridge.dto.request.UpdateProjectStatusRequest;
import com.skillbridge.dto.response.ChatMessageResponse;
import com.skillbridge.dto.response.ProjectDetailResponse;
import com.skillbridge.dto.response.ProjectSummaryResponse;
import com.skillbridge.service.ChatService;
import com.skillbridge.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ChatService    chatService;

    // ── GET all my projects ───────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(projectService.getMyProjects(email));
    }

    // ── GET single project ────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(projectService.getProjectDetail(id, email));
    }

    // ── UPDATE project status ─────────────────────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectStatusRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                projectService.updateProjectStatus(id, request.getStatus(), email));
    }

    // ── GET chat history ──────────────────────────────────────────────
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatService.getChatHistory(id, email));
    }

    // ── SEND message via REST ─────────────────────────────────────────
    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                chatService.sendMessage(id, request.getContent(), email));
    }

    // ── WebSocket message handler ─────────────────────────────────────
    @MessageMapping("/chat.{projectId}")
    public void handleWebSocketMessage(
            @DestinationVariable Long projectId,
            @Payload SendMessageRequest request,
            Principal principal) {
        if (principal != null) {
            chatService.handleWebSocketMessage(
                    projectId, request.getContent(), principal.getName());
        }
    }
}