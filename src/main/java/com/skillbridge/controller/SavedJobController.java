package com.skillbridge.controller;

import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping("/{jobId}")
    public ResponseEntity<Map<String,String>> save(
            @PathVariable Long jobId,
            @AuthenticationPrincipal String email) {
        savedJobService.saveJob(jobId, email);
        return ResponseEntity.ok(Map.of("message", "Job saved"));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String,String>> unsave(
            @PathVariable Long jobId,
            @AuthenticationPrincipal String email) {
        savedJobService.unsaveJob(jobId, email);
        return ResponseEntity.ok(Map.of("message", "Job removed from saved"));
    }

    @GetMapping
    public ResponseEntity<List<JobCardResponse>> getSaved(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(savedJobService.getSavedJobs(email));
    }

    @GetMapping("/ids")
    public ResponseEntity<Set<Long>> getSavedIds(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(savedJobService.getSavedJobIds(email));
    }
}