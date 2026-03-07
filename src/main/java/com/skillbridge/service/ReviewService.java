package com.skillbridge.service;

import com.skillbridge.dto.request.SubmitReviewRequest;
import com.skillbridge.dto.response.ReviewResponse;
import com.skillbridge.dto.response.ReviewSummaryResponse;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.Review;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.ReviewRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository  reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository    userRepository;
    private final NotificationService notificationService;

    // ── SUBMIT review (client only) ───────────────────────────────────
    @Transactional
    public ReviewResponse submitReview(SubmitReviewRequest request,
                                       String reviewerEmail) {
        User reviewer = findByEmail(reviewerEmail);
        Project project = projectRepository.findByIdWithDetails(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + request.getProjectId()));

        // Only client can review
        if (!project.getClient().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException(
                    "Only the client can submit a review.");
        }

        // Project must be completed
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new BadRequestException(
                    "You can only review completed projects.");
        }

        // One review per project
        if (reviewRepository.existsByProjectId(request.getProjectId())) {
            throw new BadRequestException(
                    "You have already reviewed this project.");
        }

        User reviewee = project.getFreelancer();

        Review review = new Review();
        review.setProject(project);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);

        // Update freelancer's avg rating and review count
        updateFreelancerRating(reviewee);

        // Notify freelancer
        notificationService.send(
                reviewee,
                com.skillbridge.entity.enums.NotificationType.SYSTEM,
                "New review from " + reviewer.getName(),
                reviewer.getName() + " gave you " + request.getRating() +
                        " star" + (request.getRating() != 1 ? "s" : "") + "!",
                "/profile.html"
        );

        log.info("Review submitted: project={} rating={} by={}",
                request.getProjectId(), request.getRating(), reviewerEmail);
        return toResponse(saved);
    }

    // ── GET reviews for a freelancer ──────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForUser(Long userId,
                                                  Pageable pageable) {
        return reviewRepository
                .findByRevieweeId(userId, pageable)
                .map(this::toResponse);
    }

    // ── GET review summary for a freelancer ───────────────────────────
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long userId) {
        Page<Review> page = reviewRepository.findByRevieweeId(
                userId, Pageable.unpaged());

        ReviewSummaryResponse summary = new ReviewSummaryResponse();
        summary.setTotalReviews(reviewRepository.countByRevieweeId(userId));
        summary.setAverageRating(
                reviewRepository.findAverageRatingByUserId(userId)
                        .map(d -> Math.round(d * 10.0) / 10.0)
                        .orElse(0.0));

        // Count by star
        page.forEach(r -> {
            switch (r.getRating()) {
                case 5 -> summary.setFiveStar(summary.getFiveStar()  + 1);
                case 4 -> summary.setFourStar(summary.getFourStar()  + 1);
                case 3 -> summary.setThreeStar(summary.getThreeStar()+ 1);
                case 2 -> summary.setTwoStar(summary.getTwoStar()    + 1);
                case 1 -> summary.setOneStar(summary.getOneStar()    + 1);
            }
        });

        return summary;
    }

    // ── GET review for a specific project ─────────────────────────────
    @Transactional(readOnly = true)
    public ReviewResponse getReviewForProject(Long projectId,
                                              String email) {
        User user = findByEmail(email);
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Only client or freelancer of that project can see it
        boolean isClient     = project.getClient().getId().equals(user.getId());
        boolean isFreelancer = project.getFreelancer().getId().equals(user.getId());
        if (!isClient && !isFreelancer) {
            throw new AccessDeniedException("Access denied.");
        }

        return reviewRepository.findByProjectId(projectId)
                .map(this::toResponse)
                .orElse(null);
    }

    // ── Update avg rating on User ─────────────────────────────────────
    private void updateFreelancerRating(User freelancer) {
        double avg = reviewRepository
                .findAverageRatingByUserId(freelancer.getId())
                .orElse(0.0);
        long count = reviewRepository.countByRevieweeId(freelancer.getId());

        freelancer.setAvgRating(Math.round(avg * 10.0) / 10.0);
        freelancer.setReviewCount((int) count);
        userRepository.save(freelancer);
    }

    // ── Mapper ────────────────────────────────────────────────────────
    private ReviewResponse toResponse(Review r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setCreatedAt(r.getCreatedAt());

        if (r.getProject() != null) {
            res.setProjectId(r.getProject().getId());
            res.setProjectTitle(r.getProject().getTitle());
        }
        if (r.getReviewer() != null) {
            res.setReviewerId(r.getReviewer().getId());
            res.setReviewerName(r.getReviewer().getName());
            res.setReviewerAvatar(r.getReviewer().getAvatarUrl());
        }
        if (r.getReviewee() != null) {
            res.setRevieweeId(r.getReviewee().getId());
            res.setRevieweeName(r.getReviewee().getName());
        }
        return res;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }
}