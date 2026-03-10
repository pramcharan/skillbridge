package com.skillbridge.service;

import com.skillbridge.entity.PasswordResetToken;
import com.skillbridge.entity.User;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.repository.PasswordResetTokenRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository               userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService                 emailService;
    private final PasswordEncoder              passwordEncoder;

    @Value("${app.reset-token-expiry-minutes:60}")
    private int expiryMinutes;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Delete existing tokens for this user
            tokenRepository.deleteByUserId(user.getId());

            // Create new token
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(UUID.randomUUID().toString());
            prt.setUser(user);
            prt.setExpiresAt(
                    Instant.now().plusSeconds(expiryMinutes * 60L));
            prt.setIsUsed(false);
            tokenRepository.save(prt);

            // Send email — matches your 2-param signature
            emailService.sendPasswordResetEmail(
                    user.getEmail(), prt.getToken());

            log.info("Password reset requested for {}", email);
        });
    }

    @Transactional(readOnly = true)
    public void validateToken(String token) {
        // Uses your existing findByTokenAndIsUsedFalse
        PasswordResetToken prt = tokenRepository
                .findByTokenAndIsUsedFalse(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link."));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(
                    "This reset link has expired. Please request a new one.");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Uses your existing findByTokenAndIsUsedFalse
        PasswordResetToken prt = tokenRepository
                .findByTokenAndIsUsedFalse(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link."));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(
                    "This reset link has expired.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException(
                    "Password must be at least 8 characters.");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Uses your Boolean isUsed field
        prt.setIsUsed(true);
        tokenRepository.save(prt);

        log.info("Password reset successfully for {}", user.getEmail());
    }
}