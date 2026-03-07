package com.skillbridge.controller;

import com.skillbridge.dto.request.UpdateProfileRequest;
import com.skillbridge.dto.response.UserProfileResponse;
import com.skillbridge.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // ── GET my profile ────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(profileService.getMyProfile(email));
    }

    // ── GET public profile ────────────────────────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getPublicProfile(userId));
    }

    // ── UPDATE my profile ─────────────────────────────────────────────
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(profileService.updateProfile(email, request));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) throws IOException {
        return ResponseEntity.ok(profileService.uploadAvatar(email, file));
    }

    // ── SOFT DELETE ───────────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @AuthenticationPrincipal String email) {
        profileService.deactivateAccount(email);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }
}