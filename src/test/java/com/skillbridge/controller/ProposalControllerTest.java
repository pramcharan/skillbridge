package com.skillbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.dto.request.SubmitProposalRequest;
import com.skillbridge.dto.request.UpdateProposalStatusRequest;
import com.skillbridge.dto.response.ProposalResponse;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ForbiddenException;
import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.JwtUtil;
import com.skillbridge.security.OAuth2SuccessHandler;
import com.skillbridge.security.UserDetailsServiceImpl;
import com.skillbridge.service.FileStorageService;
import com.skillbridge.service.ProposalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProposalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProposalController Tests")
class ProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private ProposalService proposalService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private static final String BASE = "/api/v1/proposals";

    private ProposalResponse buildProposalResponse() {
        ProposalResponse resp = new ProposalResponse();
        resp.setId(100L);
        resp.setJobId(10L);
        resp.setJobTitle("Build REST API");
        resp.setFreelancerId(2L);
        resp.setFreelancerName("Bob Smith");
        resp.setCoverLetter("I can build this.");
        resp.setExpectedRate(800.0);
        resp.setStatus(ProposalStatus.PENDING);
        resp.setViewedByClient(false);
        resp.setCreatedAt(Instant.now());
        return resp;
    }

    @Nested
    @DisplayName("POST /api/v1/proposals")
    class SubmitProposalTests {

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("201 when freelancer submits proposal successfully")
        void submit_success_201() throws Exception {
            SubmitProposalRequest req = new SubmitProposalRequest();
            req.setJobId(10L);
            req.setCoverLetter("I can build this. I have extensive experience in Spring Boot and Java development. I can deliver high-quality code on time and meet all requirements.");
            req.setExpectedRate(800.0);

            when(proposalService.submitProposal(any(SubmitProposalRequest.class), nullable(String.class)))
                    .thenReturn(buildProposalResponse());

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.expectedRate").value(800.0));
        }

        @Test
        @WithMockUser(username = "client@example.com", roles = "CLIENT")
        @DisplayName("403 when client submits proposal")
        void submit_client_403() throws Exception {
            SubmitProposalRequest req = new SubmitProposalRequest();
            req.setJobId(10L);
            req.setCoverLetter("I want this. I have extensive experience in Spring Boot and Java development. I can deliver high-quality code on time and meet all requirements.");
            req.setExpectedRate(500.0);

            when(proposalService.submitProposal(any(SubmitProposalRequest.class), nullable(String.class)))
                    .thenThrow(new ForbiddenException("Only freelancers can submit proposals"));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("400 on duplicate proposal")
        void submit_duplicate_400() throws Exception {
            SubmitProposalRequest req = new SubmitProposalRequest();
            req.setJobId(10L);
            req.setCoverLetter("Again! I have extensive experience in Spring Boot and Java development. I can deliver high-quality code on time and meet all requirements.");
            req.setExpectedRate(800.0);

            when(proposalService.submitProposal(any(SubmitProposalRequest.class), nullable(String.class)))
                    .thenThrow(new BadRequestException("Already submitted a proposal"));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when request body is invalid")
        void submit_unauthenticated_400() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proposals/job/{jobId}")
    class ListProposalsTests {

        @Test
        @WithMockUser(username = "client@example.com", roles = "CLIENT")
        @DisplayName("200 returns proposals list for job owner")
        void list_success_200() throws Exception {
            when(proposalService.getProposalsForJob(anyLong(), nullable(String.class)))
                    .thenReturn(List.of(buildProposalResponse()));

            mockMvc.perform(get(BASE + "/job/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("403 when non-owner requests proposals")
        void list_nonOwner_403() throws Exception {
            doThrow(new ForbiddenException("Not your job"))
                    .when(proposalService).getProposalsForJob(anyLong(), nullable(String.class));

            mockMvc.perform(get(BASE + "/job/10"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/proposals/{id}/status")
    class AcceptProposalTests {

        @Test
        @WithMockUser(username = "client@example.com", roles = "CLIENT")
        @DisplayName("200 when client accepts proposal")
        void accept_success_200() throws Exception {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);

            ProposalResponse resp = buildProposalResponse();
            resp.setStatus(ProposalStatus.ACCEPTED);

            when(proposalService.updateProposalStatus(anyLong(), any(UpdateProposalStatusRequest.class), nullable(String.class)))
                    .thenReturn(resp);

            mockMvc.perform(patch(BASE + "/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("403 when freelancer tries to accept")
        void accept_freelancer_403() throws Exception {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);

            when(proposalService.updateProposalStatus(anyLong(), any(UpdateProposalStatusRequest.class), nullable(String.class)))
                    .thenThrow(new ForbiddenException("Only client can accept"));

            mockMvc.perform(patch(BASE + "/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/proposals/{id}/withdraw")
    class WithdrawProposalTests {

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("200 when freelancer withdraws own proposal")
        void withdraw_success_200() throws Exception {
            doNothing().when(proposalService).withdrawProposal(anyLong(), nullable(String.class));

            mockMvc.perform(delete(BASE + "/100/withdraw"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Proposal withdrawn successfully"));
        }

        @Test
        @WithMockUser(username = "other@example.com", roles = "FREELANCER")
        @DisplayName("403 when non-owner tries to withdraw")
        void withdraw_nonOwner_403() throws Exception {
            doThrow(new ForbiddenException("Not your proposal"))
                    .when(proposalService).withdrawProposal(anyLong(), nullable(String.class));

            mockMvc.perform(delete(BASE + "/100/withdraw"))
                    .andExpect(status().isForbidden());
        }
    }
}