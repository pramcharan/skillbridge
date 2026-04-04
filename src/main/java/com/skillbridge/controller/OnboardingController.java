package com.skillbridge.controller;

import com.skillbridge.dto.request.OnboardingRequest;
import com.skillbridge.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * POST /api/v1/profile/onboarding
     * Called by onboarding.html on final step submit.
     */
    @PostMapping("/onboarding")
    public ResponseEntity<Map<String, Object>> completeOnboarding(
            @AuthenticationPrincipal String email,
            @RequestBody OnboardingRequest request) {

        onboardingService.completeOnboarding(email, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Onboarding complete"
        ));
    }

    /**
     * GET /api/v1/profile/onboarding/status
     * Called after login to decide whether to redirect to onboarding.
     */
    @GetMapping("/onboarding/status")
    public ResponseEntity<Map<String, Object>> getOnboardingStatus(
            @AuthenticationPrincipal String email) {

        boolean complete = onboardingService.isOnboardingComplete(email);
        return ResponseEntity.ok(Map.of("onboardingComplete", complete));
    }
}
