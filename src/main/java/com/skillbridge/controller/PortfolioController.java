package com.skillbridge.controller;

import com.skillbridge.dto.request.PortfolioItemRequest;
import com.skillbridge.dto.response.PortfolioItemResponse;
import com.skillbridge.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // GET /api/v1/portfolio/user/{userId}  — public profile view
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PortfolioItemResponse>> getByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                portfolioService.getPortfolio(userId));
    }

    // GET /api/v1/portfolio/me  — my portfolio
    @GetMapping("/me")
    public ResponseEntity<List<PortfolioItemResponse>> getMine(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(
                portfolioService.getMyPortfolio(email));
    }

    // POST /api/v1/portfolio
    @PostMapping
    public ResponseEntity<PortfolioItemResponse> add(
            @AuthenticationPrincipal String email,
            @RequestBody PortfolioItemRequest req) {
        return ResponseEntity.ok(
                portfolioService.addItem(email, req));
    }

    // PUT /api/v1/portfolio/{id}
    @PutMapping("/{id}")
    public ResponseEntity<PortfolioItemResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            @RequestBody PortfolioItemRequest req) {
        return ResponseEntity.ok(
                portfolioService.updateItem(id, email, req));
    }

    // DELETE /api/v1/portfolio/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        portfolioService.deleteItem(id, email);
        return ResponseEntity.ok(
                Map.of("message", "Portfolio item deleted."));
    }
}