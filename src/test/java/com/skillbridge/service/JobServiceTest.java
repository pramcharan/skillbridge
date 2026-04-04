package com.skillbridge.service;

import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.dto.mapper.JobMapper;
import com.skillbridge.dto.request.PostJobRequest;
import com.skillbridge.dto.request.UpdateJobRequest;
import com.skillbridge.dto.request.JobAttachmentDTO;
import com.skillbridge.dto.response.JobCardResponse;
import com.skillbridge.dto.response.JobDetailResponse;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.ProposalRepository;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.service.ai.AiScoringOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Unit Tests")
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProposalRepository proposalRepository;
    @Mock private JobMapper jobMapper;
    @Mock private AiScoringOrchestrator aiScoringOrchestrator;

    @InjectMocks
    private JobService jobService;

    private User clientUser;
    private User freelancerUser;
    private User adminUser;
    private Job testJob;
    private JobDetailResponse detailResponse;
    private JobCardResponse cardResponse;

    @BeforeEach
    void setUp() {
        clientUser = new User();
        clientUser.setId(1L);
        clientUser.setEmail("client@example.com");
        clientUser.setName("Alice");
        clientUser.setRole(Role.CLIENT);
        clientUser.setIsActive(true);

        freelancerUser = new User();
        freelancerUser.setId(2L);
        freelancerUser.setEmail("freelancer@example.com");
        freelancerUser.setName("Bob");
        freelancerUser.setRole(Role.FREELANCER);
        freelancerUser.setIsActive(true);
        freelancerUser.setSkills("Java,Spring Boot,Docker");

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);

        testJob = new Job();
        testJob.setId(10L);
        testJob.setTitle("Build a REST API");
        testJob.setDescription("Need a Spring Boot developer.");
        testJob.setCategory(JobCategory.TECHNOLOGY);
        testJob.setBudget(1000.0);
        testJob.setStatus(JobStatus.OPEN);
        testJob.setClient(clientUser);
        testJob.setRequiredSkills("Java,Spring Boot");
        testJob.setCreatedAt(Instant.now());
        testJob.setProposalCount(0);

        detailResponse = new JobDetailResponse();
        detailResponse.setId(10L);
        detailResponse.setTitle("Build a REST API");
        detailResponse.setDescription("Need a Spring Boot developer.");
        detailResponse.setCategory(JobCategory.TECHNOLOGY);
        detailResponse.setBudget(1000.0);
        detailResponse.setStatus(JobStatus.OPEN);
        detailResponse.setClientName("Alice");
        detailResponse.setCreatedAt(Instant.now());

        cardResponse = new JobCardResponse();
        cardResponse.setId(10L);
        cardResponse.setTitle("Build a REST API");
        cardResponse.setCategory(JobCategory.TECHNOLOGY);
        cardResponse.setBudget(1000.0);
        cardResponse.setStatus(JobStatus.OPEN);
        cardResponse.setClientName("Alice");
        cardResponse.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("postJob()")
    class PostJobTests {

        @Test
        @DisplayName("should create job successfully")
        void postJob_success() {
            PostJobRequest req = buildPostJobRequest();

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(clientUser));
            when(jobRepository.save(any(Job.class)))
                    .thenReturn(testJob);
            when(jobMapper.toDetailResponse(testJob))
                    .thenReturn(detailResponse);

            JobDetailResponse resp = jobService.postJob(req, "client@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.getTitle()).isEqualTo("Build a REST API");

            verify(jobRepository).save(argThat(new ArgumentMatcher<Job>() {
                @Override
                public boolean matches(Job job) {
                    return job != null
                            && job.getClient() == clientUser
                            && job.getTitle().equals("Build a REST API")
                            && job.getDescription().equals("Need a Spring Boot developer.")
                            && job.getCategory() == JobCategory.TECHNOLOGY
                            && job.getRequiredSkills().equals("Java,Spring Boot")
                            && job.getBudget().equals(1000.0)
                            && job.getStatus() == JobStatus.OPEN
                            && job.getProposalCount() == 0
                            && job.getAutoExpireAt() != null;
                }
            }));
            verify(jobMapper).toDetailResponse(testJob);
        }

        @Test
        @DisplayName("should trim title and description before saving")
        void postJob_trimsFields() {
            PostJobRequest req = new PostJobRequest();
            req.setTitle("  Build a REST API  ");
            req.setDescription("  Need a Spring Boot developer.  ");
            req.setCategory(JobCategory.TECHNOLOGY);
            req.setRequiredSkills("Java,Spring Boot");
            req.setBudget(1000.0);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(clientUser));
            when(jobRepository.save(any(Job.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jobMapper.toDetailResponse(any(Job.class)))
                    .thenReturn(detailResponse);

            jobService.postJob(req, "client@example.com");

            verify(jobRepository).save(argThat(job ->
                    job.getTitle().equals("Build a REST API") &&
                            job.getDescription().equals("Need a Spring Boot developer.")
            ));
        }

        @Test
        @DisplayName("should use provided autoExpireAt when request contains it")
        void postJob_usesProvidedAutoExpireAt() {
            Instant customExpiry = Instant.now().plusSeconds(5000);

            PostJobRequest req = buildPostJobRequest();
            req.setAutoExpireAt(customExpiry);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(clientUser));
            when(jobRepository.save(any(Job.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jobMapper.toDetailResponse(any(Job.class)))
                    .thenReturn(detailResponse);

            jobService.postJob(req, "client@example.com");

            verify(jobRepository).save(argThat(job ->
                    customExpiry.equals(job.getAutoExpireAt())
            ));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client email not found")
        void postJob_userNotFound() {
            PostJobRequest req = buildPostJobRequest();

            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.postJob(req, "missing@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getJobById()")
    class GetJobTests {

        @Test
        @DisplayName("should return job detail for existing job with anonymous view")
        void getJob_found_anonymous() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobMapper.toDetailResponse(testJob)).thenReturn(detailResponse);

            JobDetailResponse resp = jobService.getJobById(10L, null);

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(10L);
            assertThat(resp.getCategory()).isEqualTo(JobCategory.TECHNOLOGY);

            verify(aiScoringOrchestrator, never()).scoreSync(any(), any());
            verify(aiScoringOrchestrator, never()).scoreAsync(any(), any(), any());
        }

        @Test
        @DisplayName("should return job detail without AI when freelancer email not found")
        void getJob_freelancerMissing_returnsNormalResponse() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobMapper.toDetailResponse(testJob)).thenReturn(detailResponse);
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            JobDetailResponse resp = jobService.getJobById(10L, "ghost@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(10L);
            assertThat(resp.getAiPreviewScore()).isNull();

            verify(aiScoringOrchestrator, never()).scoreSync(any(), any());
        }

        @Test
        @DisplayName("should enrich job detail with AI preview for freelancer")
        void getJob_found_freelancer_aiPreview() {
            AiMatchResult aiResult = AiMatchResult.builder()
                    .finalScore(0.85)
                    .badge("STRONG_MATCH")
                    .explanation("Strong Java match")
                    .matchedSkills(List.of("Java", "Spring Boot"))
                    .missingSkills(List.of("Docker"))
                    .build();

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobMapper.toDetailResponse(testJob)).thenReturn(detailResponse);
            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancerUser));
            when(aiScoringOrchestrator.scoreSync(freelancerUser, testJob))
                    .thenReturn(aiResult);

            TransactionSynchronizationManager.initSynchronization();
            try {
                JobDetailResponse resp = jobService.getJobById(10L, "freelancer@example.com");

                assertThat(resp).isNotNull();
                assertThat(resp.getAiPreviewScore()).isEqualTo(0.85);
                assertThat(resp.getAiPreviewBadge()).isEqualTo("STRONG_MATCH");
                assertThat(resp.getAiPreviewReason()).isEqualTo("Strong Java match");
                assertThat(resp.getMatchedSkills()).contains("Java", "Spring Boot");
                assertThat(resp.getMissingSkills()).contains("Docker");

                verify(aiScoringOrchestrator).scoreSync(freelancerUser, testJob);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for missing job")
        void getJob_notFound() {
            when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.getJobById(99L, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Job not found");
        }
    }

    @Nested
    @DisplayName("getJobs()")
    class GetJobsTests {

        @Test
        @DisplayName("should return paginated job list")
        void getJobs_returnsPaginatedList() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(jobRepository.searchJobs(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, null, null, null, null, 0, 10, null
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Build a REST API");
            assertThat(result.getContent().get(0).getRequiredSkills()).contains("Java", "Spring Boot");
        }

        @Test
        @DisplayName("should return empty page when no jobs match")
        void getJobs_noMatch_returnsEmpty() {
            Page<Job> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(jobRepository.searchJobs(any(), any(), any(), any(), any()))
                    .thenReturn(empty);

            Page<JobCardResponse> result = jobService.getJobs(
                    "nonexistent", null, null, null, null, 0, 10, null
            );

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should call budget sort repository method when sortBy is budget")
        void getJobs_budgetSort() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(jobRepository.searchJobsByBudgetDesc(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, null, null, null, "budget", 0, 10, null
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(jobRepository).searchJobsByBudgetDesc(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should call proposals sort repository method when sortBy is proposals")
        void getJobs_proposalsSort() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(jobRepository.searchJobsByFewestProposals(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, null, null, null, "proposals", 0, 10, null
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(jobRepository).searchJobsByFewestProposals(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should parse valid category string")
        void getJobs_validCategory() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(jobRepository.searchJobs(any(), eq(JobCategory.TECHNOLOGY), any(), any(), any()))
                    .thenReturn(page);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, "TECHNOLOGY", null, null, "recent", 0, 10, null
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(jobRepository).searchJobs(any(), eq(JobCategory.TECHNOLOGY), any(), any(), any());
        }

        @Test
        @DisplayName("should ignore invalid category string")
        void getJobs_invalidCategory_ignored() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(jobRepository.searchJobs(any(), isNull(), any(), any(), any()))
                    .thenReturn(page);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, "INVALID_CATEGORY", null, null, "recent", 0, 10, null
            );

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(jobRepository).searchJobs(any(), isNull(), any(), any(), any());
        }

        @Test
        @DisplayName("should add AI preview for freelancer browsing jobs")
        void getJobs_withFreelancer_addsAiPreview() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            AiMatchResult aiResult = AiMatchResult.builder()
                    .finalScore(0.76)
                    .badge("GOOD_MATCH")
                    .explanation("Good fit")
                    .build();

            when(jobRepository.searchJobs(any(), any(), any(), any(), any()))
                    .thenReturn(page);
            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancerUser));
            when(aiScoringOrchestrator.scoreSync(freelancerUser, testJob))
                    .thenReturn(aiResult);

            Page<JobCardResponse> result = jobService.getJobs(
                    null, null, null, null, "recent", 0, 10, "freelancer@example.com"
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAiPreviewScore()).isEqualTo(0.76);
            assertThat(result.getContent().get(0).getAiPreviewBadge()).isEqualTo("GOOD_MATCH");

            verify(aiScoringOrchestrator).scoreSync(freelancerUser, testJob);
        }
    }

    @Nested
    @DisplayName("getClientJobs()")
    class GetClientJobsTests {

        @Test
        @DisplayName("should return jobs of the client")
        void getClientJobs_success() {
            Page<Job> page = new PageImpl<>(List.of(testJob), PageRequest.of(0, 10), 1);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(clientUser));
            when(jobRepository.findByClient(eq(clientUser), any()))
                    .thenReturn(page);
            when(jobMapper.toCardResponse(any(Job.class)))
                    .thenReturn(cardResponse);

            Page<JobCardResponse> result = jobService.getClientJobs("client@example.com", 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Build a REST API");
        }
    }

    @Nested
    @DisplayName("updateJob()")
    class UpdateJobTests {

        @Test
        @DisplayName("should update own job successfully")
        void updateJob_success() {
            UpdateJobRequest request = new UpdateJobRequest();
            request.setTitle("Updated Title");
            request.setDescription("Updated Description");
            request.setRequiredSkills("Java,Spring Boot,Docker");
            request.setBudget(1500.0);
            request.setStatus(JobStatus.CLOSED);

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.save(any(Job.class))).thenReturn(testJob);
            when(jobMapper.toDetailResponse(any(Job.class))).thenReturn(detailResponse);

            JobDetailResponse result = jobService.updateJob(10L, request, "client@example.com");

            assertThat(result).isNotNull();
            verify(jobRepository).save(testJob);
            assertThat(testJob.getTitle()).isEqualTo("Updated Title");
            assertThat(testJob.getDescription()).isEqualTo("Updated Description");
            assertThat(testJob.getRequiredSkills()).isEqualTo("Java,Spring Boot,Docker");
            assertThat(testJob.getBudget()).isEqualTo(1500.0);
            assertThat(testJob.getStatus()).isEqualTo(JobStatus.CLOSED);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner updates job")
        void updateJob_nonOwner() {
            UpdateJobRequest request = new UpdateJobRequest();
            request.setTitle("Updated Title");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> jobService.updateJob(10L, request, "other@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own jobs");
        }
    }

    @Nested
    @DisplayName("deleteJob()")
    class DeleteJobTests {

        @Test
        @DisplayName("should delete job when called by owning client")
        void deleteJob_success() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            jobService.deleteJob(10L, "client@example.com");

            verify(jobRepository).delete(testJob);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner tries to delete job")
        void deleteJob_nonOwner() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> jobService.deleteJob(10L, "other@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own jobs");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job missing")
        void deleteJob_notFound() {
            when(jobRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.deleteJob(10L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getSimilarJobs()")
    class GetSimilarJobsTests {

        @Test
        @DisplayName("should return similar jobs")
        void getSimilarJobs_success() {
            Job similarJob = new Job();
            similarJob.setId(11L);
            similarJob.setTitle("Build Microservice");
            similarJob.setCategory(JobCategory.TECHNOLOGY);
            similarJob.setBudget(900.0);
            similarJob.setStatus(JobStatus.OPEN);
            similarJob.setClient(clientUser);
            similarJob.setRequiredSkills("Java,Docker");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.findSimilarJobs(eq(JobCategory.TECHNOLOGY), eq("Java"), eq(10L), any(Pageable.class)))
                    .thenReturn(List.of(similarJob));
            when(jobMapper.toCardResponse(similarJob)).thenReturn(cardResponse);

            List<JobCardResponse> result = jobService.getSimilarJobs(10L);

            assertThat(result).hasSize(1);
            verify(jobRepository).findSimilarJobs(eq(JobCategory.TECHNOLOGY), eq("Java"), eq(10L), any(Pageable.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when base job not found")
        void getSimilarJobs_jobNotFound() {
            when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.getSimilarJobs(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Job not found");
        }

        @Test
        @DisplayName("should use blank skill when required skills are missing")
        void getSimilarJobs_blankSkill() {
            testJob.setRequiredSkills(null);

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.findSimilarJobs(eq(JobCategory.TECHNOLOGY), eq(""), eq(10L), any(Pageable.class)))
                    .thenReturn(List.of());

            List<JobCardResponse> result = jobService.getSimilarJobs(10L);

            assertThat(result).isEmpty();
            verify(jobRepository).findSimilarJobs(eq(JobCategory.TECHNOLOGY), eq(""), eq(10L), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("expireOldJobs()")
    class ExpireOldJobsTests {

        @Test
        @DisplayName("should mark expired open jobs as EXPIRED")
        void expireOldJobs_success() {
            Job expiredJob = new Job();
            expiredJob.setId(20L);
            expiredJob.setTitle("Old Job");
            expiredJob.setStatus(JobStatus.OPEN);

            when(jobRepository.findByStatusAndAutoExpireAtBefore(eq(JobStatus.OPEN), any()))
                    .thenReturn(List.of(expiredJob));

            jobService.expireOldJobs();

            assertThat(expiredJob.getStatus()).isEqualTo(JobStatus.EXPIRED);
            verify(jobRepository).saveAll(List.of(expiredJob));
        }

        @Test
        @DisplayName("should handle no expired jobs")
        void expireOldJobs_empty() {
            when(jobRepository.findByStatusAndAutoExpireAtBefore(eq(JobStatus.OPEN), any()))
                    .thenReturn(List.of());

            jobService.expireOldJobs();

            verify(jobRepository).saveAll(List.of());
        }
    }

    @Nested
    @DisplayName("toCardResponse()")
    class ToCardResponseTests {

        @Test
        @DisplayName("should map required skills and client name")
        void toCardResponse_success() {
            JobCardResponse result = jobService.toCardResponse(testJob);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTitle()).isEqualTo("Build a REST API");
            assertThat(result.getRequiredSkills()).contains("Java", "Spring Boot");
            assertThat(result.getClientName()).isEqualTo("Alice");
            assertThat(result.getProposalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty skills list when requiredSkills blank")
        void toCardResponse_blankSkills() {
            testJob.setRequiredSkills("   ");

            JobCardResponse result = jobService.toCardResponse(testJob);

            assertThat(result.getRequiredSkills()).isEmpty();
        }

        @Test
        @DisplayName("should default proposalCount to zero when null")
        void toCardResponse_nullProposalCount() {
            testJob.setProposalCount(null);

            JobCardResponse result = jobService.toCardResponse(testJob);

            assertThat(result.getProposalCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("attachments")
    class AttachmentTests {

        @Test
        @DisplayName("should return parsed attachments")
        void getAttachments_success() {
            testJob.setAttachmentUrls("url1,url2");
            testJob.setAttachmentNames("file1.pdf,file2.pdf");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            List<JobAttachmentDTO> result = jobService.getAttachments(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).url()).isEqualTo("url1");
            assertThat(result.get(0).name()).isEqualTo("file1.pdf");
            assertThat(result.get(1).url()).isEqualTo("url2");
            assertThat(result.get(1).name()).isEqualTo("file2.pdf");
        }

        @Test
        @DisplayName("should add attachment for owning client")
        void addAttachment_success() {
            testJob.setAttachmentUrls("url1");
            testJob.setAttachmentNames("file1.pdf");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.save(any(Job.class))).thenReturn(testJob);

            List<JobAttachmentDTO> result = jobService.addAttachment(
                    10L, "url2", "file2.pdf", "client@example.com"
            );

            assertThat(testJob.getAttachmentUrls()).isEqualTo("url1,url2");
            assertThat(testJob.getAttachmentNames()).isEqualTo("file1.pdf,file2.pdf");
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw when non-owner adds attachment")
        void addAttachment_nonOwner() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> jobService.addAttachment(
                    10L, "url2", "file2.pdf", "other@example.com"
            )).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("should throw when attachment limit exceeded")
        void addAttachment_limitExceeded() {
            testJob.setAttachmentUrls("u1,u2,u3,u4,u5");
            testJob.setAttachmentNames("f1,f2,f3,f4,f5");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> jobService.addAttachment(
                    10L, "u6", "f6", "client@example.com"
            )).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Maximum 5 attachments allowed");
        }

        @Test
        @DisplayName("should remove attachment")
        void removeAttachment_success() {
            testJob.setAttachmentUrls("url1,url2");
            testJob.setAttachmentNames("file1.pdf,file2.pdf");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.save(any(Job.class))).thenReturn(testJob);

            List<JobAttachmentDTO> result = jobService.removeAttachment(
                    10L, "url1", "client@example.com"
            );

            assertThat(testJob.getAttachmentUrls()).isEqualTo("url2");
            assertThat(testJob.getAttachmentNames()).isEqualTo("file2.pdf");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).url()).isEqualTo("url2");

        }

        @Test
        @DisplayName("should clear fields when last attachment removed")
        void removeAttachment_lastOne() {
            testJob.setAttachmentUrls("url1");
            testJob.setAttachmentNames("file1.pdf");

            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));
            when(jobRepository.save(any(Job.class))).thenReturn(testJob);

            List<JobAttachmentDTO> result = jobService.removeAttachment(
                    10L, "url1", "client@example.com"
            );

            assertThat(testJob.getAttachmentUrls()).isNull();
            assertThat(testJob.getAttachmentNames()).isNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw when non-owner removes attachment")
        void removeAttachment_nonOwner() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(testJob));

            assertThatThrownBy(() -> jobService.removeAttachment(
                    10L, "url1", "other@example.com"
            )).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Not authorised");
        }
    }

    private PostJobRequest buildPostJobRequest() {
        PostJobRequest req = new PostJobRequest();
        req.setTitle("Build a REST API");
        req.setDescription("Need a Spring Boot developer.");
        req.setCategory(JobCategory.TECHNOLOGY);
        req.setRequiredSkills("Java,Spring Boot");
        req.setBudget(1000.0);
        req.setDeadline(null);
        req.setAutoExpireAt(null);
        return req;
    }
}