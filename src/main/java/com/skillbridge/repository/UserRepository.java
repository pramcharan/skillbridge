package com.skillbridge.repository;

import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Count by role
    long countByRole(com.skillbridge.entity.enums.Role role);

    // Count active users
    long countByIsActiveTrue();

    // Find all with pagination — admin view
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findAllForAdmin(Pageable pageable);

    // Search users by name or email
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) " +
            "ORDER BY u.createdAt DESC")
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);
}