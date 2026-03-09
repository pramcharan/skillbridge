package com.skillbridge.repository;

import com.skillbridge.entity.Review;
import com.skillbridge.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByProjectIdAndReviewer(Long projectId, User reviewer);
    Page<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee = :user")
    Double calculateAvgRating(@Param("user") User user);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee = :user")
    Long countByReviewee(@Param("user") User user);

    @Query("SELECT r FROM Review r WHERE r.reviewee.role = 'FREELANCER' ORDER BY r.createdAt DESC")
    List<Review> findRecentFreelancerReviews(Pageable pageable);

    // All reviews for a freelancer — newest first
    @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.reviewer " +
            "JOIN FETCH r.project " +
            "WHERE r.reviewee.id = :userId " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findByRevieweeId(
            @Param("userId") Long userId, Pageable pageable);

    // Check if client already reviewed this project
    boolean existsByProjectId(Long projectId);

    // Average rating for a freelancer
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :userId")
    Optional<Double> findAverageRatingByUserId(@Param("userId") Long userId);

    // Count reviews for a freelancer
    long countByRevieweeId(Long userId);

    // Get review for a specific project
    @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.reviewer " +
            "JOIN FETCH r.reviewee " +
            "WHERE r.project.id = :projectId")
    Optional<Review> findByProjectId(@Param("projectId") Long projectId);

    // Platform avg rating
    @Query("SELECT AVG(r.rating) FROM Review r")
    Optional<Double> findPlatformAverageRating();

    @Query("SELECT r FROM Review r JOIN FETCH r.reviewer " +
            "WHERE r.reviewee.id = :userId ORDER BY r.createdAt DESC")
    Page<Review> findByRevieweeIdWithReviewer(
            @Param("userId") Long userId, Pageable pageable);
}