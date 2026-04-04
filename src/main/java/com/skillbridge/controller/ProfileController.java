package com.skillbridge.controller;

import com.skillbridge.dto.request.UpdateProfileRequest;
import com.skillbridge.dto.response.UserProfileResponse;
import com.skillbridge.entity.User;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.service.ProfileService;
import jakarta.transaction.Transactional;
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
    private final UserRepository userRepository;

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

    @DeleteMapping("/deactivate")
    @Transactional
    public ResponseEntity<Map<String,String>> deactivate(
            @AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Anonymize and deactivate
        user.setIsActive(false);
        user.setName("Deleted User");
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setSkills(null);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "Account deactivated successfully."));
    }


    // DELETE /api/v1/profile/me
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal String email) {
        profileService.softDeleteAccount(email);
        return ResponseEntity.ok(
                Map.of("message", "Your account has been deleted successfully."));
    }

    // POST /api/v1/profile/me/link/{provider}?providerEmail=...
    @PostMapping("/me/link/{provider}")
    public ResponseEntity<Map<String, String>> linkProvider(
            @PathVariable String provider,
            @RequestParam String providerEmail,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                profileService.linkOAuthProvider(email, provider, providerEmail));
    }

    // DELETE /api/v1/profile/me/link/{provider}
    @DeleteMapping("/me/link/{provider}")
    public ResponseEntity<Map<String, String>> unlinkProvider(
            @PathVariable String provider,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                profileService.unlinkOAuthProvider(email, provider));
    }

    // PUT /api/v1/profile/me/role  (OAuth new user role selection)
    @PutMapping("/me/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @RequestParam String role,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(profileService.updateRole(email, role));
    }
}