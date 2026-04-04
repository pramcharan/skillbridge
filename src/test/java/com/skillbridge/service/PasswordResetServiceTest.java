package com.skillbridge.service;

import com.skillbridge.entity.PasswordResetToken;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.repository.PasswordResetTokenRepository;
import com.skillbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("ram@example.com");
        user.setRole(Role.FREELANCER);
        user.setIsActive(true);

        // Set expiryMinutes for unit test
        try {
            java.lang.reflect.Field field = PasswordResetService.class.getDeclaredField("expiryMinutes");
            field.setAccessible(true);
            field.set(passwordResetService, 60);
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    @DisplayName("requestReset should create token, delete old tokens, and send email")
    void requestReset_success() {
        when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        passwordResetService.requestReset("ram@example.com");

        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(argThat(token ->
                token.getUser().equals(user)
                        && token.getToken() != null
                        && !token.getToken().isBlank()
                        && Boolean.FALSE.equals(token.getIsUsed())
                        && token.getExpiresAt().isAfter(Instant.now())
        ));
        verify(emailService).sendPasswordResetEmail(eq("ram@example.com"), anyString());
    }

    @Test
    @DisplayName("requestReset should silently succeed for unknown email")
    void requestReset_unknownEmail_silentSuccess() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() ->
                passwordResetService.requestReset("unknown@example.com")
        );

        verify(tokenRepository, never()).deleteByUserId(anyLong());
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("validateToken should not throw for valid non-expired token")
    void validateToken_valid_noException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(1500));
        token.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("valid-token"))
                .thenReturn(Optional.of(token));

        assertThatNoException().isThrownBy(() ->
                passwordResetService.validateToken("valid-token")
        );
    }

    @Test
    @DisplayName("validateToken should throw for expired token")
    void validateToken_expired_throws() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("old-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().minusSeconds(100));
        token.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("old-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.validateToken("old-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This reset link has expired. Please request a new one.");
    }

    @Test
    @DisplayName("validateToken should throw for already-used or invalid token")
    void validateToken_used_throws() {
        when(tokenRepository.findByTokenAndIsUsedFalse("used-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.validateToken("used-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired reset link.");
    }

    @Test
    @DisplayName("resetPassword should update password and mark token used")
    void resetPassword_success() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(1500));
        token.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("valid-token"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword1!"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.resetPassword("valid-token", "NewPassword1!");

        verify(userRepository).save(argThat(u ->
                "encoded-password".equals(u.getPasswordHash())
        ));
        verify(tokenRepository).save(argThat(t ->
                Boolean.TRUE.equals(t.getIsUsed())
        ));
    }

    @Test
    @DisplayName("resetPassword should throw BadRequestException for invalid token")
    void resetPassword_invalidToken_throws() {
        when(tokenRepository.findByTokenAndIsUsedFalse("bad-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "NewPass1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired reset link.");
    }

    @Test
    @DisplayName("resetPassword should throw BadRequestException for expired token")
    void resetPassword_expiredToken_throws() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().minusSeconds(10));
        token.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("expired-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "NewPass1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This reset link has expired.");
    }

    @Test
    @DisplayName("resetPassword should throw BadRequestException for short password")
    void resetPassword_shortPassword_throws() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(1500));
        token.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("valid-token"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "short"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Password must be at least 8 characters.");
    }
}