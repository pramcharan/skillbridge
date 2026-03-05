package com.skillbridge.security;

import com.skillbridge.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1. Validate signature and expiry
                if (!jwtUtil.validateToken(token)) {
                    log.debug("Invalid JWT token for request: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Check if token has been revoked (logout blacklist)
                String tokenHash = hashToken(token);
                if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
                    log.debug("Revoked JWT token used for: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. Extract claims and set authentication
                String email  = jwtUtil.extractEmail(token);
                String role   = jwtUtil.extractRole(token);
                Long   userId = jwtUtil.extractUserId(token);

                if (email != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {

                    var authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    var authToken = new UsernamePasswordAuthenticationToken(
                            email, null, authorities
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Store userId in request for easy access in controllers
                    request.setAttribute("userId", userId);

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (Exception e) {
                log.error("JWT processing error: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    // ── Extract Bearer token from Authorization header ──────────────
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // ── SHA-256 hash the token before storing in DB ─────────────────
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}