package com.skillbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.dto.request.PostJobRequest;
import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.dto.response.JobDetailResponse;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.exception.ForbiddenException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.security.JwtAuthFilter;
import com.skillbridge.security.OAuth2SuccessHandler;
import com.skillbridge.security.UserDetailsServiceImpl;
import com.skillbridge.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JobController Tests")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JobRepository jobRepository;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    private static final String BASE = "/api/v1/jobs";

    private JobDetailResponse buildJobDetailResponse() {
        JobDetailResponse resp = new JobDetailResponse();
        resp.setId(10L);
        resp.setTitle("Build REST API");
        resp.setDescription("Spring Boot dev needed");
        resp.setCategory(JobCategory.TECHNOLOGY);
        resp.setBudget(1000.0);
        resp.setRequiredSkills(List.of("Java", "Spring Boot"));
        resp.setStatus(JobStatus.OPEN);
        resp.setClientName("Alice");
        resp.setProposalCount(0);
        resp.setCreatedAt(Instant.now());
        return resp;
    }

    private JobCardResponse buildJobCardResponse() {
        JobCardResponse resp = new JobCardResponse();
        resp.setId(10L);
        resp.setTitle("Build REST API");
        resp.setCategory(JobCategory.TECHNOLOGY);
        resp.setBudget(1000.0);
        resp.setStatus(JobStatus.OPEN);
        resp.setRequiredSkills(List.of("Java"));
        resp.setClientName("Alice");
        resp.setCreatedAt(Instant.now());
        resp.setProposalCount(0);
        return resp;
    }

    @Nested
    @DisplayName("POST /api/v1/jobs")
    class PostJobTests {

        @Test
        @WithMockUser(username = "client@example.com", roles = "CLIENT")
        @DisplayName("201 when client creates job successfully")
        void postJob_success_201() throws Exception {
            PostJobRequest req = new PostJobRequest();
            req.setTitle("Build REST API");
            req.setDescription("Spring Boot needed and more text to make it at least 30 characters long.");
            req.setCategory(JobCategory.TECHNOLOGY);
            req.setRequiredSkills("Java");
            req.setBudget(1000.0);

            when(jobService.postJob(any(), any())).thenReturn(buildJobDetailResponse());

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.title").value("Build REST API"))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @WithMockUser(username = "freelancer@example.com", roles = "FREELANCER")
        @DisplayName("403 when freelancer tries to post job")
        void postJob_freelancer_403() throws Exception {
            PostJobRequest req = new PostJobRequest();
            req.setTitle("Build REST API");
            req.setDescription("Spring Boot needed and more text to make it at least 30 characters long.");
            req.setCategory(JobCategory.TECHNOLOGY);
            req.setRequiredSkills("Java");
            req.setBudget(1000.0);

            when(jobService.postJob(any(), any()))
                    .thenThrow(new ForbiddenException("Forbidden"));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 when unauthenticated user sends invalid request")
        void postJob_unauthenticated_400() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jobs/{id}")
    class GetJobTests {

        @Test
        @DisplayName("200 for existing job")
        void getJob_success_200() throws Exception {
            when(jobService.getJobById(eq(10L), any())).thenReturn(buildJobDetailResponse());

            mockMvc.perform(get(BASE + "/10").with(anonymous()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.category").value("TECHNOLOGY"))
                    .andExpect(jsonPath("$.requiredSkills[0]").value("Java"));
        }

        @Test
        @DisplayName("404 for non-existent job")
        void getJob_notFound_404() throws Exception {
            when(jobService.getJobById(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Not found"));

            mockMvc.perform(get(BASE + "/999").with(anonymous()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jobs")
    class BrowseJobsTests {

        @Test
        @DisplayName("200 with paginated results")
        void browseJobs_200() throws Exception {
            var page = new PageImpl<>(
                    List.of(buildJobCardResponse()),
                    PageRequest.of(0, 10), 1
            );

            when(jobService.getJobs(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE)
                            .param("page", "0")
                            .param("size", "10")
                            .with(anonymous()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Build REST API"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("200 with category filter applied")
        void browseJobs_withCategoryFilter() throws Exception {
            var page = new PageImpl<>(List.of(buildJobCardResponse()), PageRequest.of(0, 10), 1);

            when(jobService.getJobs(any(), eq("TECHNOLOGY"), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE)
                            .param("category", "TECHNOLOGY")
                            .with(anonymous()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].category").value("TECHNOLOGY"));
        }

        @Test
        @DisplayName("200 empty page when no results match")
        void browseJobs_empty_200() throws Exception {
            var empty = new PageImpl<JobCardResponse>(List.of(), PageRequest.of(0, 10), 0);

            when(jobService.getJobs(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(empty);

            mockMvc.perform(get(BASE)
                            .param("keyword", "nonexistent-xyz")
                            .with(anonymous()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/jobs/{id}")
    class DeleteJobTests {

        @Test
        @WithMockUser(username = "client@example.com", roles = "CLIENT")
        @DisplayName("200 when owner deletes job")
        void deleteJob_success_200() throws Exception {
            doNothing().when(jobService).deleteJob(10L, "client@example.com");

            mockMvc.perform(delete(BASE + "/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Job deleted successfully"));
        }

        @Test
        @DisplayName("200 even when unauthenticated because filters are disabled and service is mocked")
        void deleteJob_unauthenticated_currentBehavior() throws Exception {
            doNothing().when(jobService).deleteJob(eq(10L), any());

            mockMvc.perform(delete(BASE + "/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Job deleted successfully"));
        }
    }
}