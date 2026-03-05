package com.skillbridge.service;

import com.skillbridge.dto.request.*;
import com.skillbridge.dto.response.AuthResponse;
import com.skillbridge.entity.*;
import com.skillbridge.entity.enums.*;
import com.skillbridge.exception.*;
import com.skillbridge.repository.*;
import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository              userRepository;
    private final RevokedTokenRepository      revokedTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder             passwordEncoder;
    private final JwtUtil                     jwtUtil;
    private final EmailService                emailService;

    // ── REGISTER ────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Duplicate email check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with this email already exists.");
        }

        // 2. Validate role
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role must be FREELANCER or CLIENT.");
        }
        if (role == Role.ADMIN) {
            throw new BadRequestException("Cannot register as ADMIN.");
        }

        // 3. Create user
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        user.setAvgRating(0.0);
        user.setReviewCount(0);
        user.setProfileCompletionPct(calculateCompletion(user));

        User saved = userRepository.save(user);
        log.info("New user registered: {} ({})", saved.getEmail(), saved.getRole());

        // 4. Send welcome email (async — won't block registration)
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        // 5. Generate JWT and return
        String token = jwtUtil.generateToken(
                saved.getEmail(), saved.getRole().name(), saved.getId());

        return new AuthResponse(
                token, saved.getRole().name(),
                saved.getId(), saved.getName(), saved.getEmail());
    }

    // ── LOGIN ────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {

        // 1. Find user
        User user = userRepository.findByEmail(
                        request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException(
                        "Invalid email or password."));

        // 2. Check account is active
        if (!user.getIsActive()) {
            throw new BadRequestException(
                    "This account has been deactivated. Contact support.");
        }

        // 3. OAuth-only users have no password
        if (user.getPasswordHash() == null) {
            throw new BadRequestException(
                    "This account uses Google or GitHub login. " +
                            "Please sign in with your OAuth provider.");
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password.");
        }

        // 5. Update last active
        user.setLastActive(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getId());

        return new AuthResponse(
                token, user.getRole().name(),
                user.getId(), user.getName(), user.getEmail());
    }

    // ── LOGOUT ───────────────────────────────────────────────────────
    @Transactional
    public void logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) return;

        String token = bearerToken.substring(7);
        if (!jwtUtil.validateToken(token)) return;

        // Blacklist the token
        String tokenHash  = JwtAuthFilter.hashToken(token);
        if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
            RevokedToken revoked = new RevokedToken();
            revoked.setTokenHash(tokenHash);
            revoked.setExpiresAt(jwtUtil.extractExpiration(token).toInstant());
            revokedTokenRepository.save(revoked);
            log.info("Token revoked for user: {}",
                    jwtUtil.extractEmail(token));
        }
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success — never reveal if email exists
        userRepository.findByEmail(
                        request.getEmail().toLowerCase().trim())
                .ifPresent(user -> {
                    // Delete any existing unused tokens for this user
                    var existing = resetTokenRepository
                            .findByTokenAndIsUsedFalse("dummy");
                    // Generate new token
                    String token = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    resetToken.setToken(token);
                    resetToken.setExpiresAt(
                            Instant.now().plus(1, ChronoUnit.HOURS));
                    resetToken.setIsUsed(false);
                    resetTokenRepository.save(resetToken);

                    emailService.sendPasswordResetEmail(user.getEmail(), token);
                    log.info("Password reset token generated for: {}",
                            user.getEmail());
                });
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndIsUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset token."));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(
                    "This reset link has expired. Request a new one.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("Password reset completed for: {}", user.getEmail());
    }

    // ── PROFILE COMPLETION CALCULATOR ────────────────────────────────
    public int calculateCompletion(User user) {
        int score = 0;
        if (user.getName()       != null && !user.getName().isBlank())   score += 20;
        if (user.getBio()        != null && !user.getBio().isBlank())    score += 20;
        if (user.getSkills()     != null && !user.getSkills().isBlank()) score += 20;
        if (user.getHourlyRate() != null)                                score += 20;
        if (user.getAvatarUrl()  != null && !user.getAvatarUrl().isBlank()) score += 20;
        return score;
    }
}