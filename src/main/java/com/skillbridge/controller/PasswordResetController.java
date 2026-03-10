package com.skillbridge.controller;

import com.skillbridge.service.PasswordResetService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // POST /api/v1/auth/password/forgot
    @PostMapping("/forgot")
    public ResponseEntity<Map<String,String>> forgotPassword(
            @RequestBody ForgotRequest req) {
        passwordResetService.requestReset(req.getEmail());
        // Always return success (don't reveal if email exists)
        return ResponseEntity.ok(Map.of("message",
                "If that email exists, a reset link has been sent."));
    }

    // GET /api/v1/auth/password/validate?token=xxx
    @GetMapping("/validate")
    public ResponseEntity<Map<String,String>> validateToken(
            @RequestParam String token) {
        passwordResetService.validateToken(token);
        return ResponseEntity.ok(Map.of("message", "Token is valid."));
    }

    // POST /api/v1/auth/password/reset
    @PostMapping("/reset")
    public ResponseEntity<Map<String,String>> resetPassword(
            @RequestBody ResetRequest req) {
        passwordResetService.resetPassword(
                req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message",
                "Password reset successfully. You can now log in."));
    }

    @Data static class ForgotRequest {
        private String email;
    }

    @Data static class ResetRequest {
        private String token;
        private String newPassword;
    }
}