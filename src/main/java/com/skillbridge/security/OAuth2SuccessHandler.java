package com.skillbridge.security;

import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.AvailabilityStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Extract email — works for both Google and GitHub
        String email = extractEmail(oauthUser);
        String name  = extractName(oauthUser);
        String providerId = extractProviderId(oauthUser, request);
        String provider   = detectProvider(request);

        if (email == null) {
            log.error("OAuth2 login: could not extract email from provider {}", provider);
            response.sendRedirect("/login.html?error=no_email");
            return;
        }

        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Link provider ID if not already linked
            if ("google".equals(provider) && user.getGoogleId() == null) {
                user.setGoogleId(providerId);
                userRepository.save(user);
            } else if ("github".equals(provider) && user.getGithubId() == null) {
                user.setGithubId(providerId);
                userRepository.save(user);
            }
        } else {
            // New OAuth user — needs role selection
            user = new User();
            user.setEmail(email);
            user.setName(name != null ? name : email.split("@")[0]);
            user.setIsActive(true);
            user.setIsEmailVerified(true);
            user.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            user.setAvgRating(0.0);
            user.setReviewCount(0);
            user.setProfileCompletionPct(0);

            if ("google".equals(provider)) user.setGoogleId(providerId);
            else                           user.setGithubId(providerId);

            // Temporarily set role to null — user selects it on next page
            user.setRole(Role.FREELANCER); // Default, user can change
            user = userRepository.save(user);

            // Redirect to role selection for new users
            String tempToken = jwtUtil.generateToken(
                    user.getEmail(), user.getRole().name(), user.getId());
            response.sendRedirect(
                    "/register.html?oauth=true&token=" + tempToken + "&name=" + encode(name));
            return;
        }

        if (!user.getIsActive()) {
            response.sendRedirect("/login.html?error=deactivated");
            return;
        }

        // Generate JWT and redirect to dashboard
        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getId());

        String dashboard = switch (user.getRole()) {
            case FREELANCER -> "/dashboard-freelancer.html";
            case CLIENT     -> "/dashboard-client.html";
            case ADMIN      -> "/admin.html";
        };

        response.sendRedirect(dashboard + "?token=" + token);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String extractEmail(OAuth2User user) {
        // Google provides "email" directly
        String email = user.getAttribute("email");
        if (email != null) return email;
        // GitHub may provide it in different formats
        Object emailObj = user.getAttribute("email");
        return emailObj != null ? emailObj.toString() : null;
    }

    private String extractName(OAuth2User user) {
        String name = user.getAttribute("name");
        if (name != null) return name;
        name = user.getAttribute("login"); // GitHub username
        return name;
    }

    private String extractProviderId(OAuth2User user, HttpServletRequest request) {
        Object id = user.getAttribute("sub"); // Google
        if (id != null) return id.toString();
        id = user.getAttribute("id");         // GitHub
        return id != null ? id.toString() : null;
    }

    private String detectProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("google")) return "google";
        if (uri.contains("github")) return "github";
        // Check referer as fallback
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("google")) return "google";
        return "github";
    }

    private String encode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}