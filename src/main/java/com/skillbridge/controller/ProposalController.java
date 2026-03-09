package com.skillbridge.controller;

import com.skillbridge.dto.request.SubmitProposalRequest;
import com.skillbridge.dto.request.UpdateProposalStatusRequest;
import com.skillbridge.dto.response.ProposalResponse;
import com.skillbridge.dto.response.ProposalSummaryResponse;
import com.skillbridge.service.FileStorageService;
import com.skillbridge.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final FileStorageService fileStorageService;

    // ── SUBMIT proposal (FREELANCER) ──────────────────────────────────
    @PostMapping
    public ResponseEntity<ProposalResponse> submitProposal(
            @Valid @RequestBody SubmitProposalRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.submitProposal(request, email));
    }

    // ── GET proposals for a job (CLIENT) ──────────────────────────────
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ProposalResponse>> getProposalsForJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                proposalService.getProposalsForJob(jobId, email));
    }

    // ── GET single proposal ────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ProposalResponse> getProposalById(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                proposalService.getProposalById(id, email));
    }

    // ── GET my proposals (FREELANCER) ─────────────────────────────────
    @GetMapping("/my-proposals")
    public ResponseEntity<Page<ProposalSummaryResponse>> getMyProposals(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                proposalService.getMyProposals(email, page, size));
    }

    @PostMapping("/attachment")
    public ResponseEntity<Map<String,String>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) {
        try {
            String url = fileStorageService.storePortfolioFile(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "File upload failed: " + e.getMessage()));
        }
    }

    // ── UPDATE status — accept/reject (CLIENT) ─────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProposalResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProposalStatusRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                proposalService.updateProposalStatus(id, request, email));
    }

    // ── WITHDRAW proposal (FREELANCER) ─────────────────────────────────
    @DeleteMapping("/{id}/withdraw")
    public ResponseEntity<Map<String, String>> withdrawProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        proposalService.withdrawProposal(id, email);
        return ResponseEntity.ok(
                Map.of("message", "Proposal withdrawn successfully"));
    }
}