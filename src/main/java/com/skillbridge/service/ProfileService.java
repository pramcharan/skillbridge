package com.skillbridge.service;

import com.skillbridge.dto.request.UpdateProfileRequest;
import com.skillbridge.dto.response.UserProfileResponse;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.ProjectRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AuthService    authService;
    private final FileUploadService fileUploadService;
    private final JobRepository jobRepository;
    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;

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
        if (request.getLocation() != null)
            user.setLocation(request.getLocation().trim());

        // Recalculate completion percentage
        user.setProfileCompletionPct(authService.calculateCompletion(user));

        User saved = userRepository.save(user);
        log.info("Profile updated for: {}", email);
        return toResponse(saved);
    }

    // Add this new method inside the class
    @Transactional
    public UserProfileResponse uploadAvatar(String email,
                                            MultipartFile file) throws IOException {
        User user = findByEmail(email);

        String url = fileUploadService.uploadAvatar(file, user.getId());
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

    // ── Soft account deletion ───────────────────────────────────────────────────
    @Transactional
    public void softDeleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long activeProjects =
                projectRepository.countByClientIdAndStatus(
                        user.getId(), ProjectStatus.ACTIVE) +
                        projectRepository.countByFreelancerIdAndStatus(
                                user.getId(), ProjectStatus.ACTIVE);

        if (activeProjects > 0)
            throw new BadRequestException(
                    "Cannot delete account with active projects. " +
                            "Please complete or cancel all active projects first.");

        user.setIsActive(false);
        user.setEmail("deleted_" + user.getId() + "@deleted.skillbridge");
        user.setName("Deleted User");
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setSkills(null);
        user.setLocation(null);

        userRepository.save(user);
        log.info("Account soft-deleted for user id={}", user.getId());
    }

    // ── Link OAuth provider ─────────────────────────────────────────────────────
    @Transactional
    public Map<String, String> linkOAuthProvider(String email,
                                                 String provider,
                                                 String providerEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.findByEmail(providerEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new BadRequestException(
                        "This " + provider + " account is already " +
                                "linked to a different SkillBridge account.");
            }
        });

        switch (provider.toLowerCase()) {
            case "github" -> user.setLinkedGithub(providerEmail);
            case "google" -> user.setLinkedGoogle(providerEmail);
            default -> throw new BadRequestException("Unknown provider: " + provider);
        }

        userRepository.save(user);
        return Map.of("message", provider + " account linked successfully.");
    }

    // ── Unlink OAuth provider ───────────────────────────────────────────────────
    @Transactional
    public Map<String, String> unlinkOAuthProvider(String email, String provider) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank())
            throw new BadRequestException(
                    "Set a password before unlinking OAuth providers.");

        switch (provider.toLowerCase()) {
            case "github" -> user.setLinkedGithub(null);
            case "google" -> user.setLinkedGoogle(null);
            default -> throw new BadRequestException("Unknown provider: " + provider);
        }

        userRepository.save(user);
        return Map.of("message", provider + " account unlinked successfully.");
    }


    // ── Update role (OAuth new user flow) ──────────────────────────────────────
    @Transactional
    public Map<String, String> updateRole(String email, String roleName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            user.setRole(Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleName);
        }

        userRepository.save(user);
        return Map.of("message", "Role updated successfully.");
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
        r.setLocation(user.getLocation());
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
        r.setTotalJobs(jobRepository.countByClientId(user.getId()));
        r.setTotalProposals(proposalRepository.countByFreelancerId(user.getId()));
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