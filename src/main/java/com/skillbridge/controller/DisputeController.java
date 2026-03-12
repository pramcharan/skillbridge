package com.skillbridge.controller;

import com.skillbridge.dto.*;
import com.skillbridge.dto.request.DisputeReplyRequest;
import com.skillbridge.dto.request.DisputeRequest;
import com.skillbridge.dto.request.DisputeResolveRequest;
import com.skillbridge.dto.response.DisputeResponse;
import com.skillbridge.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    // POST /api/v1/disputes  — raise dispute
    @PostMapping
    public ResponseEntity<DisputeResponse> raise(
            @RequestBody DisputeRequest req,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.raiseDispute(email, req));
    }

    // GET /api/v1/disputes/me  — my disputes
    @GetMapping("/me")
    public ResponseEntity<List<DisputeResponse>> myDisputes(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.getMyDisputes(email));
    }

    // GET /api/v1/disputes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DisputeResponse> getDispute(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.getDispute(id, email));
    }

    // POST /api/v1/disputes/{id}/reply  — respondent replies
    @PostMapping("/{id}/reply")
    public ResponseEntity<DisputeResponse> reply(
            @PathVariable Long id,
            @RequestBody DisputeReplyRequest req,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.submitReply(id, email, req));
    }

    // POST /api/v1/disputes/{id}/resolve  — admin resolves
    @PostMapping("/{id}/resolve")
    public ResponseEntity<DisputeResponse> resolve(
            @PathVariable Long id,
            @RequestBody DisputeResolveRequest req,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.resolveDispute(id, email, req));
    }

    // GET /api/v1/disputes/admin/all  — admin view
    @GetMapping("/admin/all")
    public ResponseEntity<List<DisputeResponse>> adminAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                disputeService.getAllDisputes(page, size));
    }
}