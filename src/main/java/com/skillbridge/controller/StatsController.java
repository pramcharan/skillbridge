package com.skillbridge.controller;

import com.skillbridge.repository.*;
import com.skillbridge.entity.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository     userRepository;
    private final JobRepository      jobRepository;
    private final ProjectRepository  projectRepository;
    private final ReviewRepository   reviewRepository;

    /**
     * Public stats for homepage — no auth required
     * Returns: totalUsers, freelancers, clients, jobs, projects, avgRating
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        long totalUsers   = userRepository.count();
        long freelancers  = userRepository.findByRole(
                com.skillbridge.entity.enums.Role.FREELANCER).size();
        long clients      = userRepository.findByRole(
                com.skillbridge.entity.enums.Role.CLIENT).size();
        long totalJobs    = jobRepository.count();
        long totalProjects= projectRepository.count();
        long completed    = projectRepository.countByStatus(ProjectStatus.COMPLETED);

        // Average rating across all reviews
        var allReviews    = reviewRepository.findAll();
        double avgRating  = allReviews.stream()
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);

        // Jobs by category
        List<Object[]> categoryCounts = jobRepository.countByCategory();
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Object[] row : categoryCounts) {
            byCategory.add(Map.of(
                    "category", row[0].toString(),
                    "count",    row[1]
            ));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",     totalUsers);
        stats.put("freelancers",    freelancers);
        stats.put("clients",        clients);
        stats.put("jobs",           totalJobs);
        stats.put("projects",       totalProjects);
        stats.put("completed",      completed);
        stats.put("avgRating",      Math.round(avgRating * 10.0) / 10.0);
        stats.put("jobsByCategory", byCategory);

        return ResponseEntity.ok(stats);
    }

    /**
     * Category job counts — for flip cards on homepage
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategoryStats() {
        List<Object[]> rows = jobRepository.countByCategory();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(Map.of(
                    "category", row[0].toString(),
                    "count",    row[1]
            ));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Top 4 freelancers by rating — for homepage featured section
     * Only shows freelancers who have at least 1 review
     */
    @GetMapping("/top-freelancers")
    public ResponseEntity<List<Map<String, Object>>> getTopFreelancers() {
        var freelancers = userRepository.findTopFreelancers();

        List<Map<String, Object>> result = new ArrayList<>();
        for (var f : freelancers) {
            // Only include if they have at least one review
            if (f.getReviewCount() == null || f.getReviewCount() == 0) continue;

            Map<String, Object> data = new HashMap<>();
            data.put("id",          f.getId());
            data.put("name",        f.getName());
            data.put("category",    f.getSkills() != null
                    ? f.getSkills().split(",")[0].trim() : "Freelancer");
            data.put("skills",      f.getSkills() != null
                    ? Arrays.asList(f.getSkills().split(",")) : List.of());
            data.put("hourlyRate",  f.getHourlyRate());
            data.put("avgRating",   f.getAvgRating());
            data.put("reviewCount", f.getReviewCount());
            result.add(data);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Recent reviews for homepage testimonials
     * Only shows reviews from completed projects
     */
    @GetMapping("/reviews")
    public ResponseEntity<List<Map<String, Object>>> getRecentReviews() {
        var reviews = reviewRepository.findRecentFreelancerReviews(
                org.springframework.data.domain.PageRequest.of(0, 6));

        List<Map<String, Object>> result = new ArrayList<>();
        for (var r : reviews) {
            if (r.getComment() == null || r.getComment().isBlank()) continue;
            Map<String, Object> data = new HashMap<>();
            data.put("reviewerName", r.getReviewer().getName());
            data.put("reviewerRole", r.getReviewer().getRole().name());
            data.put("comment",      r.getComment());
            data.put("rating",       r.getRating());
            result.add(data);
        }
        return ResponseEntity.ok(result);
    }
}