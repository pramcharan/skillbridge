package com.skillbridge.controller;

import com.skillbridge.dto.response.*;
import com.skillbridge.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}