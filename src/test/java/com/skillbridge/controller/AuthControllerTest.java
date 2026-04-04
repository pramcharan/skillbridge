package com.skillbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.dto.request.LoginRequest;
import com.skillbridge.dto.request.RegisterRequest;
import com.skillbridge.dto.response.AuthResponse;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.DuplicateResourceException;
import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.OAuth2SuccessHandler;
import com.skillbridge.security.UserDetailsServiceImpl;
import com.skillbridge.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    private static final String BASE = "/api/v1/auth";

    @Nested
    @DisplayName("POST /register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("201 on valid registration")
        void register_success_201() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("Ram Kumar");
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");
            req.setRole("FREELANCER");

            AuthResponse resp = AuthResponse.builder()
                    .token("jwt.token")
                    .email("ram@example.com")
                    .name("Ram Kumar")
                    .role("FREELANCER")
                    .onboardingComplete(false)
                    .userId(1L)
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt.token"))
                    .andExpect(jsonPath("$.email").value("ram@example.com"))
                    .andExpect(jsonPath("$.onboardingComplete").value(false));
        }

        @Test
        @DisplayName("409 on duplicate email")
        void register_duplicateEmail_409() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("Dup User");
            req.setEmail("dup@example.com");
            req.setPassword("Password1!");
            req.setRole("CLIENT");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("An account with this email already exists."));

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("An account with this email already exists."));
        }

        @Test
        @DisplayName("400 on missing required fields")
        void register_missingFields_400() throws Exception {
            String badJson = """
                { "name": "", "email": "", "password": "x", "role": "" }
                """;

            mockMvc.perform(post(BASE + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /login")
    class LoginEndpointTests {

        @Test
        @DisplayName("200 on valid credentials")
        void login_success_200() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("Password1!");

            AuthResponse resp = AuthResponse.builder()
                    .token("jwt.token")
                    .email("ram@example.com")
                    .name("Ram Kumar")
                    .role("FREELANCER")
                    .onboardingComplete(true)
                    .userId(1L)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(resp);

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt.token"))
                    .andExpect(jsonPath("$.role").value("FREELANCER"))
                    .andExpect(jsonPath("$.onboardingComplete").value(true));
        }

        @Test
        @DisplayName("400 on unknown email")
        void login_unknownEmail_400() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@example.com");
            req.setPassword("pass");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadRequestException("Invalid email or password."));

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }

        @Test
        @DisplayName("400 on wrong password")
        void login_wrongPassword_400() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("ram@example.com");
            req.setPassword("WrongPass");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadRequestException("Invalid password"));

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid password"));
        }
    }
}