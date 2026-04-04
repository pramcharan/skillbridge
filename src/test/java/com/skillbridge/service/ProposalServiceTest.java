package com.skillbridge.service;

import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.dto.mapper.ProposalMapper;
import com.skillbridge.dto.request.SubmitProposalRequest;
import com.skillbridge.dto.request.UpdateProposalStatusRequest;
import com.skillbridge.dto.response.ProposalResponse;
import com.skillbridge.dto.response.ProposalSummaryResponse;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.Proposal;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobStatus;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.ProposalStatus;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.JobRepository;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.ProposalRepository;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.service.ai.AiScoringOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProposalService Unit Tests")
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProposalMapper proposalMapper;

    @Mock
    private AiScoringOrchestrator aiScoringOrchestrator;

    @InjectMocks
    private ProposalService proposalService;

    private User freelancer;
    private User client;
    private User otherUser;
    private Job openJob;
    private Proposal existingProposal;
    private ProposalResponse proposalResponse;
    private AiMatchResult aiMatchResult;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setName("Client User");

        freelancer = new User();
        freelancer.setId(2L);
        freelancer.setEmail("freelancer@example.com");
        freelancer.setName("Freelancer User");
        freelancer.setSkills("Java,Spring Boot,MySQL");
        freelancer.setProfileCompletionPct(80);

        otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other User");

        openJob = new Job();
        openJob.setId(10L);
        openJob.setTitle("Backend API");
        openJob.setDescription("Build Spring Boot backend");
        openJob.setStatus(JobStatus.OPEN);
        openJob.setClient(client);
        openJob.setRequiredSkills("Java,Spring Boot");
        openJob.setProposalCount(0);
        openJob.setBudget(1000.0);

        existingProposal = new Proposal();
        existingProposal.setId(100L);
        existingProposal.setJob(openJob);
        existingProposal.setFreelancer(freelancer);
        existingProposal.setStatus(ProposalStatus.PENDING);
        existingProposal.setCoverLetter("I can build this.");
        existingProposal.setExpectedRate(800.0);
        existingProposal.setViewedByClient(false);

        proposalResponse = new ProposalResponse();
        proposalResponse.setId(100L);
        proposalResponse.setStatus(ProposalStatus.PENDING);

        aiMatchResult = AiMatchResult.builder()
                .finalScore(85.0)
                .badge("Strong Match")
                .explanation("Good match")
                .provider("sync")
                .build();
    }

    @Nested
    @DisplayName("submitProposal()")
    class SubmitProposalTests {

        @Test
        @DisplayName("should submit proposal successfully")
        void submit_success() {
            SubmitProposalRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(jobRepository.findById(10L))
                    .thenReturn(Optional.of(openJob));
            when(proposalRepository.existsByJobIdAndFreelancerId(10L, 2L))
                    .thenReturn(false);
            when(aiScoringOrchestrator.scoreSync(freelancer, openJob))
                    .thenReturn(aiMatchResult);
            when(proposalRepository.save(any(Proposal.class)))
                    .thenAnswer(inv -> {
                        Proposal p = inv.getArgument(0);
                        if (p.getId() == null) {
                            p.setId(100L);
                        }
                        return p;
                    });
            when(proposalMapper.toResponse(any(Proposal.class)))
                    .thenReturn(proposalResponse);
            when(aiScoringOrchestrator.scoreAsync(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(aiMatchResult));

            TransactionSynchronizationManager.initSynchronization();
            try {
                ProposalResponse resp = proposalService.submitProposal(req, "freelancer@example.com");

                assertThat(resp).isNotNull();
                assertThat(resp.getStatus()).isEqualTo(ProposalStatus.PENDING);

                ArgumentCaptor<Proposal> proposalCaptor = ArgumentCaptor.forClass(Proposal.class);
                verify(proposalRepository, atLeastOnce()).save(proposalCaptor.capture());

                Proposal savedProposal = proposalCaptor.getAllValues().get(0);
                assertThat(savedProposal.getJob()).isEqualTo(openJob);
                assertThat(savedProposal.getFreelancer()).isEqualTo(freelancer);
                assertThat(savedProposal.getCoverLetter()).isEqualTo("I am perfect for this job.");
                assertThat(savedProposal.getExpectedRate()).isEqualTo(800.0);
                assertThat(savedProposal.getStatus()).isEqualTo(ProposalStatus.PENDING);
                assertThat(savedProposal.getViewedByClient()).isFalse();
                assertThat(savedProposal.getAiMatchScore()).isEqualTo(85.0);
                assertThat(savedProposal.getAiMatchBadge()).isEqualTo("Strong Match");
                assertThat(savedProposal.getAiMatchReason()).isEqualTo("Good match");

                verify(jobRepository).save(argThat(job -> job.getProposalCount() == 1));
                verify(notificationService).send(
                        eq(client),
                        eq(NotificationType.NEW_PROPOSAL),
                        eq("New proposal received"),
                        contains("applied to your job"),
                        eq("/proposals-client.html?jobId=10")
                );

                List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                assertThat(synchronizations).hasSize(1);

                synchronizations.forEach(TransactionSynchronization::afterCommit);

                verify(aiScoringOrchestrator).scoreAsync(eq(freelancer), eq(openJob), any());
                verify(proposalRepository, times(1)).save(any(Proposal.class));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when freelancer not found")
        void submit_userNotFound_throws() {
            SubmitProposalRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.submitProposal(req, "freelancer@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: freelancer@example.com");
        }

        @Test
        @DisplayName("should throw BadRequestException on duplicate proposal")
        void submit_duplicate_throws() {
            SubmitProposalRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(jobRepository.findById(10L))
                    .thenReturn(Optional.of(openJob));
            when(proposalRepository.existsByJobIdAndFreelancerId(10L, 2L))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    proposalService.submitProposal(req, "freelancer@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You have already applied to this job.");
        }

        @Test
        @DisplayName("should throw BadRequestException when job is not open")
        void submit_closedJob_throws() {
            SubmitProposalRequest req = buildRequest();
            openJob.setStatus(JobStatus.CLOSED);

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(jobRepository.findById(10L))
                    .thenReturn(Optional.of(openJob));

            assertThatThrownBy(() ->
                    proposalService.submitProposal(req, "freelancer@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This job is no longer accepting proposals.");
        }

        @Test
        @DisplayName("should throw BadRequestException when freelancer applies to own job")
        void submit_ownJob_throws() {
            SubmitProposalRequest req = buildRequest();
            openJob.setClient(freelancer);

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(jobRepository.findById(10L))
                    .thenReturn(Optional.of(openJob));
            when(proposalRepository.existsByJobIdAndFreelancerId(10L, 2L))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    proposalService.submitProposal(req, "freelancer@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You cannot apply to your own job.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job not found")
        void submit_jobNotFound_throws() {
            SubmitProposalRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(jobRepository.findById(10L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.submitProposal(req, "freelancer@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Job not found: 10");
        }
    }

    @Nested
    @DisplayName("getProposalsForJob()")
    class ListProposalsTests {

        @Test
        @DisplayName("should return proposals when client is owner")
        void list_success() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(openJob));
            when(proposalRepository.findByJobIdOrderByAiMatchScoreDesc(10L))
                    .thenReturn(List.of(existingProposal));

            ProposalResponse mapped = new ProposalResponse();
            mapped.setId(100L);
            mapped.setStatus(ProposalStatus.PENDING);

            when(proposalMapper.toResponse(existingProposal)).thenReturn(mapped);
            when(proposalRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<ProposalResponse> result =
                    proposalService.getProposalsForJob(10L, "client@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);

            verify(proposalRepository).saveAll(argThat(list ->
                    ((List<Proposal>) list).stream().allMatch(Proposal::getViewedByClient)
            ));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner lists proposals")
        void list_nonOwner_throws() {
            when(jobRepository.findById(10L)).thenReturn(Optional.of(openJob));

            assertThatThrownBy(() ->
                    proposalService.getProposalsForJob(10L, "freelancer@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You can only view proposals for your own jobs.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when job not found")
        void list_jobNotFound_throws() {
            when(jobRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.getProposalsForJob(10L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Job not found: 10");
        }
    }

    @Nested
    @DisplayName("getProposalById()")
    class GetProposalByIdTests {

        @Test
        @DisplayName("should allow freelancer owner to view proposal")
        void getById_freelancer_success() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));
            when(proposalMapper.toResponse(existingProposal))
                    .thenReturn(proposalResponse);

            ProposalResponse result =
                    proposalService.getProposalById(100L, "freelancer@example.com");

            assertThat(result).isEqualTo(proposalResponse);
        }

        @Test
        @DisplayName("should allow client owner to view proposal")
        void getById_client_success() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));
            when(proposalMapper.toResponse(existingProposal))
                    .thenReturn(proposalResponse);

            ProposalResponse result =
                    proposalService.getProposalById(100L, "client@example.com");

            assertThat(result).isEqualTo(proposalResponse);
        }

        @Test
        @DisplayName("should throw AccessDeniedException for unrelated user")
        void getById_unrelatedUser_throws() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.getProposalById(100L, "other@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You do not have permission to view this proposal.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when proposal not found")
        void getById_notFound_throws() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.getProposalById(100L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Proposal not found: 100");
        }
    }

    @Nested
    @DisplayName("getMyProposals()")
    class GetMyProposalsTests {

        @Test
        @DisplayName("should return paged proposal summaries")
        void getMyProposals_success() {
            ProposalSummaryResponse summary = new ProposalSummaryResponse();
            Page<Proposal> proposalPage = new PageImpl<>(List.of(existingProposal));

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(proposalRepository.findByFreelancerId(eq(2L), any(PageRequest.class)))
                    .thenReturn(proposalPage);
            when(proposalMapper.toSummary(existingProposal)).thenReturn(summary);

            Page<ProposalSummaryResponse> result =
                    proposalService.getMyProposals("freelancer@example.com", 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(summary);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when freelancer not found")
        void getMyProposals_userNotFound_throws() {
            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.getMyProposals("freelancer@example.com", 0, 10))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: freelancer@example.com");
        }
    }

    @Nested
    @DisplayName("updateProposalStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should accept proposal and create project")
        void accept_success() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);

            Proposal otherProposal = new Proposal();
            otherProposal.setId(101L);
            otherProposal.setJob(openJob);
            User otherFreelancer = new User();
            otherFreelancer.setId(22L);
            otherFreelancer.setEmail("otherfreelancer@example.com");
            otherFreelancer.setName("Other Freelancer");
            otherProposal.setFreelancer(otherFreelancer);
            otherProposal.setStatus(ProposalStatus.PENDING);

            ProposalResponse acceptedResponse = new ProposalResponse();
            acceptedResponse.setId(100L);
            acceptedResponse.setStatus(ProposalStatus.ACCEPTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));
            when(proposalRepository.findByJobIdAndStatus(10L, ProposalStatus.PENDING))
                    .thenReturn(List.of(existingProposal, otherProposal));
            when(proposalRepository.save(any(Proposal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(proposalRepository.saveAll(anyList()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(proposalMapper.toResponse(any(Proposal.class)))
                    .thenReturn(acceptedResponse);

            ProposalResponse result =
                    proposalService.updateProposalStatus(100L, req, "client@example.com");

            assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            verify(jobRepository).save(argThat(job -> job.getStatus() == JobStatus.CLOSED));

            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(projectCaptor.capture());

            Project savedProject = projectCaptor.getValue();
            assertThat(savedProject.getClient()).isEqualTo(client);
            assertThat(savedProject.getFreelancer()).isEqualTo(freelancer);
            assertThat(savedProject.getProposal()).isEqualTo(existingProposal);
            assertThat(savedProject.getJob()).isEqualTo(openJob);
            assertThat(savedProject.getTitle()).isEqualTo("Backend API");
            assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.ACTIVE);

            verify(notificationService, atLeastOnce()).send(any(), any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should reject proposal successfully")
        void reject_success() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.REJECTED);

            ProposalResponse rejectedResponse = new ProposalResponse();
            rejectedResponse.setId(100L);
            rejectedResponse.setStatus(ProposalStatus.REJECTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));
            when(proposalRepository.save(any(Proposal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(proposalMapper.toResponse(any(Proposal.class)))
                    .thenReturn(rejectedResponse);

            ProposalResponse result =
                    proposalService.updateProposalStatus(100L, req, "client@example.com");

            assertThat(result.getStatus()).isEqualTo(ProposalStatus.REJECTED);
            verify(notificationService).send(
                    eq(freelancer),
                    eq(NotificationType.PROPOSAL_UPDATE),
                    eq("Application update"),
                    contains("was not selected"),
                    eq("/dashboard-freelancer.html")
            );
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner updates proposal")
        void update_nonOwner_throws() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.updateProposalStatus(100L, req, "freelancer@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You can only manage proposals for your own jobs.");
        }

        @Test
        @DisplayName("should throw BadRequestException when proposal already processed")
        void update_alreadyProcessed_throws() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);
            existingProposal.setStatus(ProposalStatus.ACCEPTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.updateProposalStatus(100L, req, "client@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This proposal has already been accepted.");
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid status")
        void update_invalidStatus_throws() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.WITHDRAWN);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.updateProposalStatus(100L, req, "client@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid status. Use ACCEPTED or REJECTED.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when proposal not found")
        void update_notFound_throws() {
            UpdateProposalStatusRequest req = new UpdateProposalStatusRequest();
            req.setStatus(ProposalStatus.ACCEPTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.updateProposalStatus(100L, req, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Proposal not found: 100");
        }
    }

    @Nested
    @DisplayName("withdrawProposal()")
    class WithdrawTests {

        @Test
        @DisplayName("should withdraw pending proposal")
        void withdraw_pending_success() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));
            when(proposalRepository.save(any(Proposal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            proposalService.withdrawProposal(100L, "freelancer@example.com");

            verify(proposalRepository).save(argThat(p -> p.getStatus() == ProposalStatus.WITHDRAWN));
            verify(jobRepository).save(argThat(job -> job.getProposalCount() == 0));
            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.PROPOSAL_UPDATE),
                    eq("Proposal withdrawn"),
                    contains("withdrew their proposal"),
                    eq("/proposals-client.html?jobId=10")
            );
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner tries to withdraw")
        void withdraw_nonOwner_throws() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.withdrawProposal(100L, "other@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You can only withdraw your own proposals.");
        }

        @Test
        @DisplayName("should throw BadRequestException when withdrawing non-pending proposal")
        void withdraw_nonPending_throws() {
            existingProposal.setStatus(ProposalStatus.ACCEPTED);

            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(existingProposal));

            assertThatThrownBy(() ->
                    proposalService.withdrawProposal(100L, "freelancer@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You can only withdraw pending proposals.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when proposal not found")
        void withdraw_notFound_throws() {
            when(proposalRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    proposalService.withdrawProposal(100L, "freelancer@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Proposal not found: 100");
        }
    }

    private SubmitProposalRequest buildRequest() {
        SubmitProposalRequest req = new SubmitProposalRequest();
        req.setJobId(10L);
        req.setCoverLetter("I am perfect for this job.");
        req.setExpectedRate(800.0);
        req.setAttachmentUrl(null);
        return req;
    }
}