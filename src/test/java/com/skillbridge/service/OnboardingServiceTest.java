package com.skillbridge.service;

import com.skillbridge.dto.request.OnboardingRequest;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService Unit Tests")
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    private User freelancer;

    @BeforeEach
    void setUp() {
        freelancer = new User();
        freelancer.setId(1L);
        freelancer.setEmail("ram@example.com");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setOnboardingComplete(false);
    }

    @Test
    @DisplayName("should complete onboarding and set freelancer fields")
    void completeOnboarding_freelancer_success() {
        OnboardingRequest req = new OnboardingRequest(
                "Ram", "Kumar", "I build APIs", "Hyderabad", "English",
                List.of("Java", "Spring Boot"), 50.0,
                "MID", "FULL_TIME", "https://github.com/ram",
                null, null, null, true
        );

        when(userRepository.findByEmail("ram@example.com"))
                .thenReturn(Optional.of(freelancer));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        onboardingService.completeOnboarding("ram@example.com", req);

        verify(userRepository).save(argThat(u ->
                u.isOnboardingComplete()
                        && "Ram Kumar".equals(u.getName())
                        && "I build APIs".equals(u.getBio())
                        && "Hyderabad".equals(u.getLocation())
                        && "English".equals(u.getLanguage())
                        && "Java,Spring Boot".equals(u.getSkills())
                        && u.getHourlyRate() == 50.0
                        && "MID".equals(u.getExperienceLevel())
                        && "FULL_TIME".equals(u.getAvailability())
                        && "https://github.com/ram".equals(u.getPortfolioUrl())
        ));
    }

    @Test
    @DisplayName("should complete onboarding for client role with company fields")
    void completeOnboarding_client_success() {
        freelancer.setRole(Role.CLIENT);

        OnboardingRequest req = new OnboardingRequest(
                "Priya", "Sharma", "I hire great devs", "Bangalore", "English",
                null, null, null, null, null,
                "TechCorp", "ONGOING", "5000-10000", true
        );

        when(userRepository.findByEmail("ram@example.com"))
                .thenReturn(Optional.of(freelancer));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        onboardingService.completeOnboarding("ram@example.com", req);

        verify(userRepository).save(argThat(u ->
                u.isOnboardingComplete()
                        && "Priya Sharma".equals(u.getName())
                        && "I hire great devs".equals(u.getBio())
                        && "Bangalore".equals(u.getLocation())
                        && "English".equals(u.getLanguage())
                        && "TechCorp".equals(u.getCompanyName())
                        && "ONGOING".equals(u.getHiringGoal())
                        && "5000-10000".equals(u.getBudgetRange())
        ));
    }

    @Test
    @DisplayName("should throw RuntimeException for unknown user")
    void completeOnboarding_unknownUser_throws() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        OnboardingRequest req = new OnboardingRequest(
                "X", "Y", null, null, null,
                null, null, null, null, null,
                null, null, null, true
        );

        assertThatThrownBy(() -> onboardingService.completeOnboarding("unknown@example.com", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("isOnboardingComplete should return false for pending user")
    void isOnboardingComplete_false() {
        when(userRepository.findByEmail("ram@example.com"))
                .thenReturn(Optional.of(freelancer));

        assertThat(onboardingService.isOnboardingComplete("ram@example.com")).isFalse();
    }

    @Test
    @DisplayName("isOnboardingComplete should return true for completed user")
    void isOnboardingComplete_true() {
        freelancer.setOnboardingComplete(true);

        when(userRepository.findByEmail("ram@example.com"))
                .thenReturn(Optional.of(freelancer));

        assertThat(onboardingService.isOnboardingComplete("ram@example.com")).isTrue();
    }

    @Test
    @DisplayName("isOnboardingComplete should return true for unknown user")
    void isOnboardingComplete_unknownUser_returnsTrue() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThat(onboardingService.isOnboardingComplete("missing@example.com")).isTrue();
    }
}