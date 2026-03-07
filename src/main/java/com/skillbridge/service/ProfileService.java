package com.skillbridge.service;

import com.skillbridge.dto.request.UpdateProfileRequest;
import com.skillbridge.dto.response.UserProfileResponse;
import com.skillbridge.entity.User;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.ProposalRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AuthService    authService;
    // Add to the existing fields at top
    private final FileStorageService fileStorageService;
    private final JobRepository jobRepository;
    private final ProposalRepository proposalRepository;

    // ── GET my profile ────────────────────────────────────────────────
    public UserProfileResponse getMyProfile(String email) {
        User user = findByEmail(email);
        user.setLastActive(Instant.now());
        userRepository.save(user);
        return toResponse(user);
    }

    // ── GET public profile (by userId) ────────────────────────────────
    public UserProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));
        return toResponse(user);
    }

    // ── UPDATE my profile ─────────────────────────────────────────────
    @Transactional
    public UserProfileResponse updateProfile(String email,
                                             UpdateProfileRequest request) {
        User user = findByEmail(email);

        if (request.getName()               != null) user.setName(request.getName().trim());
        if (request.getBio()                != null) user.setBio(request.getBio().trim());
        if (request.getSkills()             != null) user.setSkills(request.getSkills());
        if (request.getHourlyRate()         != null) user.setHourlyRate(request.getHourlyRate());
        if (request.getAvatarUrl()          != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getAvailabilityStatus() != null) user.setAvailabilityStatus(
                request.getAvailabilityStatus());

        // Recalculate completion percentage
        user.setProfileCompletionPct(authService.calculateCompletion(user));

        User saved = userRepository.save(user);
        log.info("Profile updated for: {}", email);
        return toResponse(saved);
    }

    // Add this new method inside the class
    @Transactional
    public UserProfileResponse uploadAvatar(String email, MultipartFile file) throws IOException {
        User user = findByEmail(email);

        // Delete old avatar if it's a stored file
        if (user.getAvatarUrl() != null &&
                user.getAvatarUrl().startsWith("/uploads/")) {
            fileStorageService.delete(user.getAvatarUrl());
        }

        String url = fileStorageService.storeAvatar(file);
        user.setAvatarUrl(url);
        user.setProfileCompletionPct(authService.calculateCompletion(user));

        return toResponse(userRepository.save(user));
    }

    // ── SOFT DELETE ───────────────────────────────────────────────────
    @Transactional
    public void deactivateAccount(String email) {
        User user = findByEmail(email);
        user.setIsActive(false);
        user.setEmail("deleted_" + user.getId() + "@removed.com");
        userRepository.save(user);
        log.info("Account deactivated: userId={}", user.getId());
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    public UserProfileResponse toResponse(User user) {
        UserProfileResponse r = new UserProfileResponse();
        r.setId(user.getId());
        r.setName(user.getName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());
        r.setBio(user.getBio());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setSkills(splitSkills(user.getSkills()));
        r.setHourlyRate(user.getHourlyRate());
        r.setAvgRating(user.getAvgRating());
        r.setReviewCount(user.getReviewCount());
        r.setProfileCompletionPct(user.getProfileCompletionPct());
        r.setAvailabilityStatus(user.getAvailabilityStatus());
        r.setIsEmailVerified(user.getIsEmailVerified());
        r.setCreatedAt(user.getCreatedAt());
        r.setLastActive(user.getLastActive());
        return r;
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) return List.of();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}