package com.skillbridge.controller;

import com.skillbridge.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    // These will be fully implemented in Day 6
    // Stubs here just so the app compiles and runs

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Register endpoint — coming Day 6"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Login endpoint — coming Day 6"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout endpoint — coming Day 6"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Forgot password — coming Day 6"));
    }
}