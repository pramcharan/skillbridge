package com.skillbridge.controller;

import com.skillbridge.dto.*;
import com.skillbridge.dto.request.CommunityMessageRequest;
import com.skillbridge.dto.response.CommunityMessageResponse;
import com.skillbridge.dto.response.PresenceResponse;
import com.skillbridge.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // REST — Get recent messages
    @GetMapping("/{room}/messages")
    public ResponseEntity<List<CommunityMessageResponse>> getMessages(
            @PathVariable String room,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                communityService.getMessages(room, email));
    }

    // REST — Get pinned messages
    @GetMapping("/{room}/pinned")
    public ResponseEntity<List<CommunityMessageResponse>> getPinned(
            @PathVariable String room) {
        return ResponseEntity.ok(
                communityService.getPinned(room));
    }

    // REST — Get presence
    @GetMapping("/{room}/presence")
    public ResponseEntity<PresenceResponse> getPresence(
            @PathVariable String room) {
        return ResponseEntity.ok(
                communityService.getPresence(room));
    }

    // REST — Join room
    @PostMapping("/{room}/join")
    public ResponseEntity<PresenceResponse> joinRoom(
            @PathVariable String room,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                communityService.joinRoom(email, room));
    }

    // REST — Leave room
    @PostMapping("/{room}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String room,
            @AuthenticationPrincipal String email) {
        communityService.leaveRoom(email, room);
        return ResponseEntity.ok().build();
    }

    // REST — Toggle reaction
    @PostMapping("/messages/{messageId}/react")
    public ResponseEntity<Map<String, Object>> react(
            @PathVariable Long messageId,
            @RequestParam String emoji,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                communityService.toggleReaction(messageId, emoji, email));
    }

    // REST — Pin/Unpin (admin)
    @PostMapping("/messages/{messageId}/pin")
    public ResponseEntity<CommunityMessageResponse> togglePin(
            @PathVariable Long messageId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                communityService.togglePin(messageId, email));
    }

    // REST — Freelancer spotlight
    @GetMapping("/spotlight")
    public ResponseEntity<List<Map<String, Object>>> getSpotlight() {
        return ResponseEntity.ok(communityService.getSpotlight());
    }

    // WebSocket — Send message
    @org.springframework.messaging.handler.annotation.MessageMapping(
            "/community.send")
    public void sendMessage(
            CommunityMessageRequest req, Principal principal) {
        communityService.sendMessage(principal.getName(), req);
    }

    // WebSocket — Join room
    @org.springframework.messaging.handler.annotation.MessageMapping(
            "/community.join")
    public void joinRoomWs(
            @Payload Map<String, String> payload,
            Principal principal) {
        communityService.joinRoom(principal.getName(), payload.get("room"));
    }

    // WebSocket — Leave room
    @org.springframework.messaging.handler.annotation.MessageMapping(
            "/community.leave")
    public void leaveRoomWs(
            @Payload Map<String, String> payload,
            Principal principal) {
        communityService.leaveRoom(principal.getName(), payload.get("room"));
    }
}