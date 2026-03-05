package com.skillbridge.controller;

import com.skillbridge.dto.request.PostJobRequest;
import com.skillbridge.dto.request.UpdateJobRequest;
import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.dto.response.JobDetailResponse;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // ── POST a new job (CLIENT only) ─────────────────────────────────
    @PostMapping
    public ResponseEntity<JobDetailResponse> postJob(
            @Valid @RequestBody PostJobRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.postJob(request, email));
    }

    // ── GET all open jobs (public — no auth needed) ──────────────────
    @GetMapping
    public ResponseEntity<Page<JobCardResponse>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobCategory category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.getJobs(keyword, category, page, size));
    }

    // ── GET single job by ID ─────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<JobDetailResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    // ── GET jobs posted by current client ────────────────────────────
    @GetMapping("/my-jobs")
    public ResponseEntity<Page<JobCardResponse>> getMyJobs(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.getClientJobs(email, page, size));
    }

    // ── UPDATE job ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<JobDetailResponse> updateJob(
            @PathVariable Long id,
            @RequestBody UpdateJobRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(jobService.updateJob(id, request, email));
    }

    // ── DELETE job ───────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        jobService.deleteJob(id, email);
        return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
    }

    // ── GET similar jobs ─────────────────────────────────────────────
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<JobCardResponse>> getSimilarJobs(
            @PathVariable Long id) {
        return ResponseEntity.ok(jobService.getSimilarJobs(id));
    }
}