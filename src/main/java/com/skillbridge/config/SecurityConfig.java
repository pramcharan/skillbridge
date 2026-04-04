package com.skillbridge.config;

import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.OAuth2SuccessHandler;
import com.skillbridge.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuth2SuccessHandler   oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ── All HTML pages — JS handles auth on the client side ──────
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/register.html",
                                "/privacy.html", "/role-select.html",
                                "/dashboard-freelancer.html", "/dashboard-client.html",
                                "/admin.html", "/jobs.html", "/job-detail.html",
                                "/post-job.html", "/profile.html",
                                "/community-chat.html", "/proposals-client.html",
                                "/reset-password.html",
                                "/onboarding.html",
                                "/project.html",
                                "/my-projects.html",
                                "/community.html",
                                "/notifications.html",
                                "/portfolio.html",
                                "/forgot-password.html",
                                "/disputes.html"
                        ).permitAll()

                        .requestMatchers("/privacy.html", "/terms.html").permitAll()

                        // ── Static assets ─────────────────────────────────────────────
                        .requestMatchers("/uploads/**", "/css/**", "/js/**", "/lib/**",
                                "/images/**", "/favicon.ico").permitAll()

                        // ── Public API endpoints ──────────────────────────────────────
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/stats/public",
                                "/api/v1/stats/categories",
                                "/api/v1/stats/top-freelancers",
                                "/api/v1/stats/reviews"
                        ).permitAll()

                        // ── OAuth2 + WebSocket ────────────────────────────────────────
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        .requestMatchers(
                                "/forgot-password.html",
                                "/api/v1/auth/password/**"
                        ).permitAll()

                        // ── Admin API only ────────────────────────────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/community/**").authenticated()


                        // ── Everything else (all other /api/** routes) ────────────────
                        .anyRequest().authenticated()
                )

                // ── Return JSON errors for API routes ────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API routes → return JSON, never redirect to OAuth login
                            String path = request.getRequestURI();
                            if (path.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}"
                                );
                            } else {
                                // Non-API routes → redirect to login page
                                response.sendRedirect("/login.html");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 403 for authenticated users without the right role
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\",\"message\":\"You do not have permission to perform this action\"}"
                            );
                        })
                )

                // ── OAuth2 login ─────────────────────────────────────────
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("/login.html?error=oauth_failed")
                )

                // ── Add JWT filter before Spring's auth filter ────────────
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

//    @Bean
//    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
//        return (request, response, authException) -> {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write(
//                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}"
//            );
//        };
//    }
}