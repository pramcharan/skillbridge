package com.skillbridge.config;

import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.OAuth2SuccessHandler;
import com.skillbridge.security.UserDetailsServiceImpl;
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

                        // ── Public static files ──────────────────────────────
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/register.html",
                                "/privacy.html", "/role-select.html"
                        ).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/lib/**",
                                "/images/**", "/favicon.ico").permitAll()

                        // ── Public API endpoints ─────────────────────────────
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/stats/public",
                                "/api/v1/stats/categories",
                                "/api/v1/stats/top-freelancers",
                                "/api/v1/stats/reviews"
                        ).permitAll()

                        // ── OAuth2 endpoints ─────────────────────────────────
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // ── WebSocket handshake ──────────────────────────────
                        .requestMatchers("/ws/**").permitAll()

                        // ── Admin only ───────────────────────────────────────
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ── Client only ──────────────────────────────────────
                        .requestMatchers("/dashboard-client.html").hasRole("CLIENT")

                        // ── Freelancer only ──────────────────────────────────
                        .requestMatchers("/dashboard-freelancer.html").hasRole("FREELANCER")

                        // ── Everything else requires authentication ──────────
                        .anyRequest().authenticated()
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
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}