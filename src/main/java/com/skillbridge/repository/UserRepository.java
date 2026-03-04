package com.skillbridge.repository;

import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByGithubId(String githubId);
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'FREELANCER' AND u.isActive = true ORDER BY u.avgRating DESC LIMIT 4")
    List<User> findTopFreelancers();
}