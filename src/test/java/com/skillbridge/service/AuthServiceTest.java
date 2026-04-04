package com.skillbridge.service;

import com.skillbridge.dto.request.ForgotPasswordRequest;
import com.skillbridge.dto.request.LoginRequest;
import com.skillbridge.dto.request.RegisterRequest;
import com.skillbridge.dto.request.ResetPasswordRequest;
import com.skillbridge.dto.response.AuthResponse;
import com.skillbridge.entity.PasswordResetToken;
import com.skillbridge.entity.RevokedToken;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.AvailabilityStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.DuplicateResourceException;
import com.skillbridge.repository.PasswordResetTokenRepository;
import com.skillbridge.repository.RevokedTokenRepository;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("ram@example.com");
        testUser.setName("Ram Kumar");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole(Role.FREELANCER);
        testUser.setIsActive(true);
        testUser.setOnboardingComplete(false);
    }

    // ─────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram Kumar");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("FREELANCER");

            when(userRepository.existsByEmail("ram@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");
            doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

            AuthResponse response = authService.register(req);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("token");
            assertThat(response.getEmail()).isEqualTo("ram@example.com");
            assertThat(response.getRole()).isEqualTo("FREELANCER");
            assertThat(response.isOnboardingComplete()).isFalse();
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Ram Kumar");

            verify(userRepository).save(any(User.class));
            verify(emailService).sendWelcomeEmail("ram@example.com", "Ram Kumar");
            verify(jwtUtil).generateToken("ram@example.com", "FREELANCER", 1L);
        }

        @Test
        @DisplayName("should normalize name and email before saving")
        void register_normalizesNameAndEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setName("  Ram Kumar  ");
            req.setEmail("  RAM@EXAMPLE.COM  ");
            req.setPassword("Password1!");
            req.setRole("FREELANCER");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.register(req);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("Ram Kumar");
            assertThat(savedUser.getEmail()).isEqualTo("ram@example.com");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void register_duplicateEmail_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram Kumar");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("FREELANCER");

            when(userRepository.existsByEmail("ram@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("should encode password before saving")
        void register_encodesPassword() {
            RegisterRequest req = new RegisterRequest();
            req.setName("New User");
            req.setEmail("new@example.com");
            req.setPassword("RawPass!");
            req.setRole("CLIENT");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("RawPass!")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(3L);
                assertThat(savedUser.getPasswordHash()).isEqualTo("encoded");
                return savedUser;
            });
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.register(req);

            verify(passwordEncoder).encode("RawPass!");
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid role")
        void register_invalidRole_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("MANAGER");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("FREELANCER or CLIENT");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BadRequestException when registering as ADMIN")
        void register_adminRole_throws() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("ADMIN");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot register as ADMIN");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set default fields for newly registered user")
        void register_setsDefaultFields() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram Kumar");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("FREELANCER");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(1L);
                return savedUser;
            });
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

            authService.register(req);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getIsActive()).isTrue();
            assertThat(savedUser.getIsEmailVerified()).isFalse();
            assertThat(savedUser.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
            assertThat(savedUser.getAvgRating()).isEqualTo(0.0);
            assertThat(savedUser.getReviewCount()).isEqualTo(0);
            assertThat(savedUser.getProfileCompletionPct()).isEqualTo(20);
        }
    }

    // ─────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login with valid credentials")
        void login_success() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1!", "hashed_password")).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt.token");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            AuthResponse response = authService.login(req);

            assertThat(response.getToken()).isEqualTo("jwt.token");
            assertThat(response.getEmail()).isEqualTo("ram@example.com");
            assertThat(response.getRole()).isEqualTo("FREELANCER");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Ram Kumar");

            verify(userRepository).save(testUser);
            assertThat(testUser.getLastActive()).isNotNull();
        }

        @Test
        @DisplayName("should normalize email before login lookup")
        void login_normalizesEmail() {
            LoginRequest req = new LoginRequest();
            req.setEmail("  RAM@EXAMPLE.COM  ");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("jwt.token");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.login(req);

            verify(userRepository).findByEmail("ram@example.com");
        }

        @Test
        @DisplayName("should throw BadRequestException for unknown email")
        void login_unknownEmail_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("unknown@example.com");
            req.setPassword("pass");

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("should throw BadRequestException for wrong password")
        void login_wrongPassword_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("WrongPass");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPass", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("should throw BadRequestException for inactive account")
        void login_inactiveAccount_throws() {
            testUser.setIsActive(false);

            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("deactivated");
        }

        @Test
        @DisplayName("should throw BadRequestException for OAuth-only user without password")
        void login_oauthOnlyUser_throws() {
            testUser.setPasswordHash(null);

            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Google or GitHub login");
        }

        @Test
        @DisplayName("should include onboardingComplete flag in response")
        void login_includesOnboardingFlag() {
            testUser.setOnboardingComplete(true);

            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            AuthResponse response = authService.login(req);

            assertThat(response.isOnboardingComplete()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should do nothing when bearer token is null")
        void logout_nullToken_noAction() {
            authService.logout(null);

            verifyNoInteractions(jwtUtil, revokedTokenRepository);
        }

        @Test
        @DisplayName("should do nothing when token does not start with Bearer")
        void logout_invalidPrefix_noAction() {
            authService.logout("Invalid token");

            verifyNoInteractions(jwtUtil, revokedTokenRepository);
        }

        @Test
        @DisplayName("should do nothing when token is invalid")
        void logout_invalidJwt_noAction() {
            when(jwtUtil.validateToken("bad.token")).thenReturn(false);

            authService.logout("Bearer bad.token");

            verify(jwtUtil).validateToken("bad.token");
            verify(revokedTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("should revoke valid token when not already blacklisted")
        void logout_validToken_revokes() {
            String rawToken = "valid.jwt.token";

            when(jwtUtil.validateToken(rawToken)).thenReturn(true);
            when(revokedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
            when(jwtUtil.extractExpiration(rawToken))
                    .thenReturn(Date.from(Instant.now().plusSeconds(3600)));
            when(jwtUtil.extractEmail(rawToken)).thenReturn("ram@example.com");

            try (MockedStatic<com.skillbridge.security.JwtAuthFilter> mockedStatic =
                         mockStatic(com.skillbridge.security.JwtAuthFilter.class)) {

                mockedStatic.when(() -> com.skillbridge.security.JwtAuthFilter.hashToken(rawToken))
                        .thenReturn("hashed_token");

                authService.logout("Bearer " + rawToken);

                verify(revokedTokenRepository).save(any(RevokedToken.class));
            }
        }

        @Test
        @DisplayName("should not save token when already blacklisted")
        void logout_alreadyRevoked_noNewSave() {
            String rawToken = "valid.jwt.token";

            when(jwtUtil.validateToken(rawToken)).thenReturn(true);
            when(revokedTokenRepository.existsByTokenHash("hashed_token")).thenReturn(true);

            try (MockedStatic<com.skillbridge.security.JwtAuthFilter> mockedStatic =
                         mockStatic(com.skillbridge.security.JwtAuthFilter.class)) {

                mockedStatic.when(() -> com.skillbridge.security.JwtAuthFilter.hashToken(rawToken))
                        .thenReturn("hashed_token");

                authService.logout("Bearer " + rawToken);

                verify(revokedTokenRepository, never()).save(any());
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should create reset token and send email for existing user")
        void forgotPassword_existingUser_createsTokenAndSendsEmail() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("ram@example.com");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));

            authService.forgotPassword(req);

            verify(resetTokenRepository).deleteByUserId(testUser.getId());
            verify(resetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq("ram@example.com"), anyString());
        }

        @Test
        @DisplayName("should normalize email before forgot password lookup")
        void forgotPassword_normalizesEmail() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("  RAM@EXAMPLE.COM  ");

            when(userRepository.findByEmail("ram@example.com")).thenReturn(Optional.of(testUser));

            authService.forgotPassword(req);

            verify(userRepository).findByEmail("ram@example.com");
        }

        @Test
        @DisplayName("should do nothing for unknown email")
        void forgotPassword_unknownEmail_noException() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("ghost@example.com");

            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatCode(() -> authService.forgotPassword(req)).doesNotThrowAnyException();

            verify(resetTokenRepository, never()).deleteByUserId(anyLong());
            verify(resetTokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }
    }

    // ─────────────────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password and mark token used for valid token")
        void resetPassword_validToken_success() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("valid-token");
            req.setNewPassword("NewPassword1!");

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("valid-token");
            resetToken.setUser(testUser);
            resetToken.setExpiresAt(Instant.now().plusSeconds(3600));
            resetToken.setIsUsed(false);

            when(resetTokenRepository.findByTokenAndIsUsedFalse("valid-token"))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode("NewPassword1!")).thenReturn("new_hashed_password");

            authService.resetPassword(req);

            assertThat(testUser.getPasswordHash()).isEqualTo("new_hashed_password");
            assertThat(resetToken.getIsUsed()).isTrue();

            verify(userRepository).save(testUser);
            verify(resetTokenRepository).save(resetToken);
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid token")
        void resetPassword_invalidToken_throws() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("bad-token");
            req.setNewPassword("NewPassword1!");

            when(resetTokenRepository.findByTokenAndIsUsedFalse("bad-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid or expired reset token");
        }

        @Test
        @DisplayName("should throw BadRequestException for expired token")
        void resetPassword_expiredToken_throws() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("expired-token");
            req.setNewPassword("NewPassword1!");

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("expired-token");
            resetToken.setUser(testUser);
            resetToken.setExpiresAt(Instant.now().minusSeconds(60));
            resetToken.setIsUsed(false);

            when(resetTokenRepository.findByTokenAndIsUsedFalse("expired-token"))
                    .thenReturn(Optional.of(resetToken));

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");

            verify(userRepository, never()).save(any());
            verify(resetTokenRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────
    // CALCULATE COMPLETION
    // ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("calculateCompletion()")
    class CalculateCompletionTests {

        @Test
        @DisplayName("should return 0 for empty profile")
        void calculateCompletion_emptyProfile_returns0() {
            User user = new User();

            int result = authService.calculateCompletion(user);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("should return 20 when only name is present")
        void calculateCompletion_onlyName_returns20() {
            User user = new User();
            user.setName("Ram");

            int result = authService.calculateCompletion(user);

            assertThat(result).isEqualTo(20);
        }

        @Test
        @DisplayName("should return 100 for fully completed profile")
        void calculateCompletion_fullProfile_returns100() {
            User user = new User();
            user.setName("Ram");
            user.setBio("Java developer");
            user.setSkills("Java,Spring");
            user.setHourlyRate(20.0);
            user.setAvatarUrl("https://example.com/avatar.png");

            int result = authService.calculateCompletion(user);

            assertThat(result).isEqualTo(100);
        }
    }
}