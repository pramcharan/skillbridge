package com.skillbridge.repository;

import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Slice Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User freelancer;
    private User client;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        freelancer = new User();
        freelancer.setEmail("freelancer@test.com");
        freelancer.setName("Bob Builder");
        freelancer.setPasswordHash("hashed");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setIsActive(true);
        freelancer.setOnboardingComplete(true);
        freelancer.setSkills("Java,Spring Boot");
        freelancer.setAvgRating(4.5);

        client = new User();
        client.setEmail("client@test.com");
        client.setName("Alice Wonder");
        client.setPasswordHash("hashed");
        client.setRole(Role.CLIENT);
        client.setIsActive(true);
        client.setOnboardingComplete(false);

        userRepository.saveAll(List.of(freelancer, client));
    }

    @Test
    @DisplayName("findByEmail returns user for existing email")
    void findByEmail_exists() {
        Optional<User> found = userRepository.findByEmail("freelancer@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob Builder");
    }

    @Test
    @DisplayName("findByEmail returns empty for unknown email")
    void findByEmail_notFound() {
        Optional<User> found = userRepository.findByEmail("ghost@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail_true() {
        assertThat(userRepository.existsByEmail("client@test.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail returns false for unknown email")
    void existsByEmail_false() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    @DisplayName("findByRole returns only users with specified role")
    void findByRole() {
        List<User> freelancers = userRepository.findByRole(Role.FREELANCER);

        assertThat(freelancers).hasSize(1);
        assertThat(freelancers.get(0).getEmail()).isEqualTo("freelancer@test.com");
    }

    @Test
    @DisplayName("findTopFreelancers returns users ordered by rating")
    void findTopFreelancers() {
        User lowRated = new User();
        lowRated.setEmail("low@test.com");
        lowRated.setName("Low Rated");
        lowRated.setPasswordHash("hashed");
        lowRated.setRole(Role.FREELANCER);
        lowRated.setIsActive(true);
        lowRated.setOnboardingComplete(true);
        lowRated.setAvgRating(2.0);
        userRepository.save(lowRated);

        List<User> top = userRepository.findTopFreelancers();

        assertThat(top).hasSize(2);
        assertThat(top.get(0).getEmail()).isEqualTo("freelancer@test.com");
        assertThat(top.get(0).getAvgRating()).isGreaterThanOrEqualTo(top.get(1).getAvgRating());
    }
}