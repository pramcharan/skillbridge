package com.skillbridge.config;

import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = System.getenv("ADMIN_EMAIL");
        String password = System.getenv("ADMIN_PASSWORD");
        String name = System.getenv("ADMIN_NAME");

        // Safety: don't run if env not set
        if (email == null || password == null) {
            log.info("Admin environment variables (ADMIN_EMAIL, ADMIN_PASSWORD) not set. Skipping admin creation.");
            return;
        }

        // Prevent duplicate admin
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Admin user with email {} already exists.", email);
            return;
        }

        try {
            User admin = new User();
            admin.setName(name != null ? name : "Admin");
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            admin.setIsEmailVerified(true);
            admin.setOnboardingComplete(true);

            userRepository.save(admin);
            log.info("Admin user created securely: {}", email);
        } catch (Exception e) {
            log.error("Failed to create admin user: {}", e.getMessage());
        }
    }
}
