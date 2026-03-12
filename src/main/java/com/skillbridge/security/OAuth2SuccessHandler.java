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

        String provider   = detectProvider(request);
        String email      = extractEmail(oauthUser, provider);
        String name       = extractName(oauthUser);
        String providerId = extractProviderId(oauthUser, provider);

        if (email == null) {
            log.error("OAuth2 login: could not extract email from provider {}", provider);
            response.sendRedirect("/login.html?error=no_email");
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // Link provider ID if not already set
            boolean updated = false;
            if ("google".equals(provider) && user.getGoogleId() == null) {
                user.setGoogleId(providerId);
                updated = true;
            } else if ("github".equals(provider) && user.getGithubId() == null) {
                user.setGithubId(providerId);
                updated = true;
            }
            if (updated) userRepository.save(user);

        } else {
            // New OAuth user — create with default FREELANCER role
            user = new User();
            user.setEmail(email);
            user.setName(name != null ? name : email.split("@")[0]);
            user.setIsActive(true);
            user.setIsEmailVerified(true);
            user.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            user.setAvgRating(0.0);
            user.setReviewCount(0);
            user.setProfileCompletionPct(0);
            user.setRole(Role.FREELANCER);  // default; user can change on next page

            if ("google".equals(provider)) user.setGoogleId(providerId);
            else                           user.setGithubId(providerId);

            user = userRepository.save(user);

            // Send new user to role selection page
            String tempToken = jwtUtil.generateToken(
                    user.getEmail(), user.getRole().name(), user.getId());
            response.sendRedirect(
                    "/register.html?oauth=true&token=" + tempToken
                            + "&name=" + encode(name));
            return;
        }

        if (!user.getIsActive()) {
            response.sendRedirect("/login.html?error=deactivated");
            return;
        }

        // Existing user — generate JWT and redirect to dashboard
        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getId());

        String dashboard = switch (user.getRole()) {
            case FREELANCER -> "/dashboard-freelancer.html";
            case CLIENT     -> "/dashboard-client.html";
            case ADMIN      -> "/admin.html";
        };

        response.sendRedirect(dashboard + "?token=" + token);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractEmail(OAuth2User user, String provider) {
        String email = user.getAttribute("email");
        if (email != null && !email.isBlank()) return email;

        // GitHub: email may be null if user set it to private
        if ("github".equals(provider)) {
            String login = user.getAttribute("login");
            if (login != null) return login + "@github.placeholder";
        }
        return null;
    }

    private String extractName(OAuth2User user) {
        String name = user.getAttribute("name");
        if (name != null && !name.isBlank()) return name;
        return user.getAttribute("login");  // GitHub fallback
    }

    private String extractProviderId(OAuth2User user, String provider) {
        if ("google".equals(provider)) {
            Object sub = user.getAttribute("sub");
            return sub != null ? sub.toString() : null;
        }
        // GitHub
        Object id = user.getAttribute("id");
        return id != null ? id.toString() : null;
    }

    private String detectProvider(HttpServletRequest request) {
        // Spring Security sets the registrationId in the request URI
        String uri = request.getRequestURI();
        if (uri.contains("google")) return "google";
        if (uri.contains("github")) return "github";

        // Fallback: check the OAuth2User attributes
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