package com.skillbridge.controller;

import com.skillbridge.dto.response.*;
import com.skillbridge.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── STATS ─────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    // ── USERS ─────────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminService.getUsers(query, page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUser(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<AdminUserResponse> deactivateUser(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.setUserActive(id, false));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<AdminUserResponse> activateUser(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.setUserActive(id, true));
    }

    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<AdminUserResponse> promoteToAdmin(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToAdmin(id));
    }

    // ── JOBS ──────────────────────────────────────────────────────────
    @GetMapping("/jobs")
    public ResponseEntity<Page<AdminJobResponse>> getJobs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getJobs(page, size));
    }

    @PatchMapping("/jobs/{id}/close")
    public ResponseEntity<AdminJobResponse> forceCloseJob(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.forceCloseJob(id));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(
            @PathVariable Long id) {
        adminService.deleteJob(id);
        return ResponseEntity.ok(Map.of("message", "Job deleted"));
    }

    @GetMapping("/charts")
    public ResponseEntity<Map<String, Object>> getChartData() {
        return ResponseEntity.ok(adminService.getChartData());
    }


    @GetMapping("/ai-health")
    public ResponseEntity<Map<String, Object>> getAiHealth() {
        return ResponseEntity.ok(adminService.getAiHealthData());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminActivityResponse>> getActivity() {
        return ResponseEntity.ok(adminService.getPlatformActivity());
    }

    // ── Project chat ──────────────────────────────────────────────

    @GetMapping("/chats/project/{projectId}")
    public ResponseEntity<List<Map<String, Object>>> getProjectMessages(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(
                adminService.getProjectChatMessages(projectId, page));
    }

    @DeleteMapping("/chats/project/{projectId}/messages/{messageId}")
    public ResponseEntity<Void> deleteProjectMessage(
            @PathVariable Long projectId,
            @PathVariable Long messageId) {
        adminService.deleteProjectChatMessage(messageId, projectId);
        return ResponseEntity.noContent().build();
    }

// ── Community chat ────────────────────────────────────────────

    @GetMapping("/chats/community/{room}")
    public ResponseEntity<List<Map<String, Object>>> getCommunityMessages(
            @PathVariable String room,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(
                adminService.getCommunityRoomMessages(room, page));
    }

    @DeleteMapping("/chats/community/{room}/messages/{messageId}")
    public ResponseEntity<Void> deleteCommunityMessage(
            @PathVariable String room,
            @PathVariable Long messageId) {
        adminService.deleteCommunityMessage(messageId, room);
        return ResponseEntity.noContent().build();
    }
}