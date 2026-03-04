package com.skillbridge.repository;

import com.skillbridge.entity.Review;
import com.skillbridge.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByProjectIdAndReviewer(Long projectId, User reviewer);
    Page<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee = :user")
    Double calculateAvgRating(@Param("user") User user);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee = :user")
    Long countByReviewee(@Param("user") User user);

    @Query("SELECT r FROM Review r WHERE r.reviewee.role = 'FREELANCER' ORDER BY r.createdAt DESC")
    List<Review> findRecentFreelancerReviews(Pageable pageable);
}