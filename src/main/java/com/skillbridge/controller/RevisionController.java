package com.skillbridge.controller;

import com.skillbridge.dto.request.RevisionRequestDTO;
import com.skillbridge.dto.request.RevisionStatusUpdateDTO;
import com.skillbridge.dto.response.RevisionResponse;
import com.skillbridge.service.RevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/revisions")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping
    public ResponseEntity<List<RevisionResponse>> getRevisions(
            @PathVariable Long projectId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                revisionService.getProjectRevisions(projectId, email));
    }

    @PostMapping
    public ResponseEntity<RevisionResponse> create(
            @PathVariable Long projectId,
            @RequestBody RevisionRequestDTO dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(revisionService.createRevision(projectId, dto, email));
    }

    @PatchMapping("/{revisionId}/status")
    public ResponseEntity<RevisionResponse> updateStatus(
            @PathVariable Long projectId,
            @PathVariable Long revisionId,
            @RequestBody RevisionStatusUpdateDTO dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                revisionService.updateStatus(revisionId, dto, email));
    }
}