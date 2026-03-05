package com.skillbridge.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    // ── Generate token ──────────────────────────────────────────────
    public String generateToken(String email, String role, Long userId) {
        return Jwts.builder()
                .subject(email)
                .claim("role",   role)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Validate token ──────────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Extract email (subject) ─────────────────────────────────────
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ── Extract role ────────────────────────────────────────────────
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ── Extract userId ──────────────────────────────────────────────
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    // ── Extract expiration ──────────────────────────────────────────
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // ── Private helpers ─────────────────────────────────────────────
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // If secret is less than 64 chars, pad it for HS512
        String padded = jwtSecret;
        while (padded.length() < 64) padded += jwtSecret;
        byte[] keyBytes = padded.substring(0, 64).getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}