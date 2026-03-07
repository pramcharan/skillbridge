package com.skillbridge.controller;

import com.skillbridge.dto.request.SubmitReviewRequest;
import com.skillbridge.dto.response.ReviewResponse;
import com.skillbridge.dto.response.ReviewSummaryResponse;
import com.skillbridge.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── SUBMIT review ─────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(
            @Valid @RequestBody SubmitReviewRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                reviewService.submitReview(request, email));
    }

    // ── GET reviews for a user ────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(
                userId,
                PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    // ── GET review summary for a user ─────────────────────────────────
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                reviewService.getReviewSummary(userId));
    }

    // ── GET review for a project ──────────────────────────────────────
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ReviewResponse> getReviewForProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal String email) {
        ReviewResponse review =
                reviewService.getReviewForProject(projectId, email);
        return review != null
                ? ResponseEntity.ok(review)
                : ResponseEntity.noContent().build();
    }
}