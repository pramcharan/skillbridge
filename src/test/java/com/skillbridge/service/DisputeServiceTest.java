package com.skillbridge.service;

import com.skillbridge.dto.request.DisputeReplyRequest;
import com.skillbridge.dto.request.DisputeRequest;
import com.skillbridge.dto.request.DisputeResolveRequest;
import com.skillbridge.dto.response.DisputeResponse;
import com.skillbridge.entity.DisputeTicket;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.DisputeResolution;
import com.skillbridge.entity.enums.DisputeStatus;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.DisputeRepository;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService Unit Tests")
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DisputeService disputeService;

    private User client;
    private User freelancer;
    private User admin;
    private Job job;
    private Project activeProject;
    private DisputeTicket openDispute;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setName("Client User");
        client.setRole(Role.CLIENT);

        freelancer = new User();
        freelancer.setId(2L);
        freelancer.setEmail("freelancer@example.com");
        freelancer.setName("Freelancer User");
        freelancer.setRole(Role.FREELANCER);

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@skillbridge.com");
        admin.setName("Admin User");
        admin.setRole(Role.ADMIN);

        job = new Job();
        job.setId(10L);
        job.setTitle("Website Development");

        activeProject = new Project();
        activeProject.setId(50L);
        activeProject.setClient(client);
        activeProject.setFreelancer(freelancer);
        activeProject.setJob(job);
        activeProject.setStatus(ProjectStatus.ACTIVE);

        openDispute = new DisputeTicket();
        openDispute.setId(300L);
        openDispute.setProject(activeProject);
        openDispute.setReporter(client);
        openDispute.setRespondent(freelancer);
        openDispute.setStatus(DisputeStatus.OPEN);
        openDispute.setReason("Work not delivered on time.");
        openDispute.setDescription("The freelancer did not deliver the agreed work on time.");
        openDispute.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("raiseDispute()")
    class RaiseDisputeTests {

        @Test
        @DisplayName("client can raise dispute successfully")
        void raise_success() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Work not delivered on time.");
            req.setDescription("The freelancer did not deliver the agreed work on time.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(openDispute);
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.of(admin));

            DisputeResponse resp = disputeService.raiseDispute("client@example.com", req);

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(300L);
            assertThat(resp.getStatus()).isEqualTo("OPEN");
            assertThat(resp.getReason()).isEqualTo("Work not delivered on time.");
            assertThat(resp.getProjectId()).isEqualTo(50L);

            verify(disputeRepository).save(any(DisputeTicket.class));
            verify(notificationService, times(2))
                    .send(any(User.class), eq(NotificationType.SYSTEM), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("freelancer can raise dispute successfully")
        void raise_freelancerSuccess() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Client is unresponsive.");
            req.setDescription("The client has stopped responding for several days.");

            DisputeTicket freelancerDispute = new DisputeTicket();
            freelancerDispute.setId(301L);
            freelancerDispute.setProject(activeProject);
            freelancerDispute.setReporter(freelancer);
            freelancerDispute.setRespondent(client);
            freelancerDispute.setStatus(DisputeStatus.OPEN);
            freelancerDispute.setReason("Client is unresponsive.");
            freelancerDispute.setDescription("The client has stopped responding for several days.");

            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 2L)).thenReturn(Optional.empty());
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(freelancerDispute);
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.of(admin));

            DisputeResponse resp = disputeService.raiseDispute("freelancer@example.com", req);

            assertThat(resp).isNotNull();
            assertThat(resp.getStatus()).isEqualTo("OPEN");
            assertThat(resp.getReporterId()).isEqualTo(2L);
            assertThat(resp.getRespondentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should trim reason and description before saving")
        void raise_trimsFields() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("  Work not delivered on time.  ");
            req.setDescription("  The freelancer did not deliver the agreed work on time.  ");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.of(admin));
            when(disputeRepository.save(any(DisputeTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

            disputeService.raiseDispute("client@example.com", req);

            verify(disputeRepository).save(argThat(ticket ->
                    "Work not delivered on time.".equals(ticket.getReason()) &&
                            "The freelancer did not deliver the agreed work on time.".equals(ticket.getDescription())
            ));
        }

        @Test
        @DisplayName("should keep evidence urls when provided")
        void raise_withEvidenceUrls_success() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Work not delivered on time.");
            req.setDescription("The freelancer did not deliver the agreed work on time.");
            req.setEvidenceUrls("https://a.com/1.png, https://a.com/2.png");

            DisputeTicket savedTicket = new DisputeTicket();
            savedTicket.setId(300L);
            savedTicket.setProject(activeProject);
            savedTicket.setReporter(client);
            savedTicket.setRespondent(freelancer);
            savedTicket.setStatus(DisputeStatus.OPEN);
            savedTicket.setReason("Work not delivered on time.");
            savedTicket.setDescription("The freelancer did not deliver the agreed work on time.");
            savedTicket.setEvidenceUrls("https://a.com/1.png, https://a.com/2.png");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(savedTicket);
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.of(admin));

            DisputeResponse resp = disputeService.raiseDispute("client@example.com", req);

            assertThat(resp.getEvidenceUrls()).containsExactly("https://a.com/1.png", "https://a.com/2.png");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void raise_userNotFound_throws() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Work not delivered on time.");
            req.setDescription("The freelancer did not deliver the agreed work on time.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.raiseDispute("client@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when project not found")
        void raise_projectNotFound_throws() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Work not delivered on time.");
            req.setDescription("The freelancer did not deliver the agreed work on time.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.raiseDispute("client@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found");
        }

        @Test
        @DisplayName("should throw BadRequestException for non-participant")
        void raise_nonParticipant_throws() {
            User stranger = new User();
            stranger.setId(99L);
            stranger.setEmail("stranger@example.com");
            stranger.setName("Stranger");
            stranger.setRole(Role.CLIENT);

            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("I want to dispute.");
            req.setDescription("I am not part of this project but trying to raise dispute.");

            when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));

            assertThatThrownBy(() -> disputeService.raiseDispute("stranger@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not a participant");
        }

        @Test
        @DisplayName("should throw BadRequestException when open dispute already exists")
        void raise_duplicateDispute_throws() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Another dispute.");
            req.setDescription("This is another detailed dispute description.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.of(openDispute));

            assertThatThrownBy(() -> disputeService.raiseDispute("client@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("open dispute");
        }

        @Test
        @DisplayName("should allow new dispute when old dispute is resolved")
        void raise_previousResolvedDispute_allowsNewOne() {
            openDispute.setStatus(DisputeStatus.RESOLVED);

            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("New issue");
            req.setDescription("This is a new detailed dispute after previous resolution.");

            DisputeTicket savedTicket = new DisputeTicket();
            savedTicket.setId(302L);
            savedTicket.setProject(activeProject);
            savedTicket.setReporter(client);
            savedTicket.setRespondent(freelancer);
            savedTicket.setStatus(DisputeStatus.OPEN);
            savedTicket.setReason("New issue");
            savedTicket.setDescription("This is a new detailed dispute after previous resolution.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(savedTicket);
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.of(admin));

            DisputeResponse resp = disputeService.raiseDispute("client@example.com", req);

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(302L);
            verify(disputeRepository).save(any(DisputeTicket.class));
        }

        @Test
        @DisplayName("should throw BadRequestException when reason is blank")
        void raise_blankReason_throws() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("   ");
            req.setDescription("The freelancer did not deliver the agreed work on time.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.raiseDispute("client@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("reason is required");
        }

        @Test
        @DisplayName("should throw BadRequestException when description is too short")
        void raise_shortDescription_throws() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Issue");
            req.setDescription("Too short");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.raiseDispute("client@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("min 20 chars");
        }

        @Test
        @DisplayName("should notify only respondent when admin does not exist")
        void raise_noAdmin_notifiesOnlyRespondent() {
            DisputeRequest req = new DisputeRequest();
            req.setProjectId(50L);
            req.setReason("Work not delivered on time.");
            req.setDescription("The freelancer did not deliver the agreed work on time.");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(disputeRepository.findByProjectIdAndReporterId(50L, 1L)).thenReturn(Optional.empty());
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(openDispute);
            when(userRepository.findFirstByRole(Role.ADMIN)).thenReturn(Optional.empty());

            disputeService.raiseDispute("client@example.com", req);

            verify(notificationService, times(1))
                    .send(eq(freelancer), eq(NotificationType.SYSTEM), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("submitReply()")
    class SubmitReplyTests {

        @Test
        @DisplayName("respondent can reply successfully")
        void submitReply_success() {
            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("I have completed most of the work and shared the files.");
            req.setEvidenceUrls("https://reply.com/a.png, https://reply.com/b.png");

            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));

            DisputeTicket savedTicket = new DisputeTicket();
            savedTicket.setId(300L);
            savedTicket.setProject(activeProject);
            savedTicket.setReporter(client);
            savedTicket.setRespondent(freelancer);
            savedTicket.setStatus(DisputeStatus.UNDER_REVIEW);
            savedTicket.setReason(openDispute.getReason());
            savedTicket.setDescription(openDispute.getDescription());
            savedTicket.setRespondentReply("I have completed most of the work and shared the files.");
            savedTicket.setRespondentEvidenceUrls("https://reply.com/a.png, https://reply.com/b.png");

            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(savedTicket);

            DisputeResponse response = disputeService.submitReply(300L, "freelancer@example.com", req);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(response.getRespondentReply()).isEqualTo("I have completed most of the work and shared the files.");
            assertThat(response.getRespondentEvidenceUrls())
                    .containsExactly("https://reply.com/a.png", "https://reply.com/b.png");

            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.SYSTEM),
                    eq("Dispute reply received"),
                    anyString(),
                    eq("/disputes.html?id=300")
            );
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dispute not found for reply")
        void submitReply_disputeNotFound_throws() {
            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("Reply text");

            when(disputeRepository.findById(300L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.submitReply(300L, "freelancer@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dispute not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when respondent user not found")
        void submitReply_userNotFound_throws() {
            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("Reply text");

            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.submitReply(300L, "freelancer@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when non-respondent tries to reply")
        void submitReply_nonRespondent_throws() {
            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("Reply text");

            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));

            assertThatThrownBy(() -> disputeService.submitReply(300L, "client@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only the respondent");
        }

        @Test
        @DisplayName("should throw BadRequestException when dispute is already resolved")
        void submitReply_resolvedDispute_throws() {
            openDispute.setStatus(DisputeStatus.RESOLVED);

            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("Reply text");

            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));

            assertThatThrownBy(() -> disputeService.submitReply(300L, "freelancer@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already closed");
        }

        @Test
        @DisplayName("should throw BadRequestException when dispute is already closed")
        void submitReply_closedDispute_throws() {
            openDispute.setStatus(DisputeStatus.CLOSED);

            DisputeReplyRequest req = new DisputeReplyRequest();
            req.setReply("Reply text");

            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));

            assertThatThrownBy(() -> disputeService.submitReply(300L, "freelancer@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already closed");
        }
    }

    @Nested
    @DisplayName("resolveDispute()")
    class ResolveDisputeTests {

        @Test
        @DisplayName("admin can resolve open dispute")
        void resolve_success() {
            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("FAVOUR_REPORTER");
            req.setAdminNotes("Reporter wins.");

            DisputeTicket savedTicket = new DisputeTicket();
            savedTicket.setId(300L);
            savedTicket.setProject(activeProject);
            savedTicket.setReporter(client);
            savedTicket.setRespondent(freelancer);
            savedTicket.setStatus(DisputeStatus.RESOLVED);
            savedTicket.setResolution(DisputeResolution.FAVOUR_REPORTER);
            savedTicket.setAdminNotes("Reporter wins.");
            savedTicket.setResolvedBy(admin);
            savedTicket.setResolvedAt(Instant.now());
            savedTicket.setReason(openDispute.getReason());
            savedTicket.setDescription(openDispute.getDescription());

            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.of(admin));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(savedTicket);

            DisputeResponse response = disputeService.resolveDispute(300L, "admin@skillbridge.com", req);

            assertThat(response.getStatus()).isEqualTo("RESOLVED");
            assertThat(response.getResolution()).isEqualTo("FAVOUR_REPORTER");
            assertThat(response.getAdminNotes()).isEqualTo("Reporter wins.");
            assertThat(response.getResolvedByName()).isEqualTo("Admin User");

            verify(disputeRepository).save(argThat(d ->
                    d.getStatus() == DisputeStatus.RESOLVED &&
                            d.getResolution() == DisputeResolution.FAVOUR_REPORTER &&
                            "Reporter wins.".equals(d.getAdminNotes()) &&
                            d.getResolvedBy() == admin &&
                            d.getResolvedAt() != null
            ));

            verify(notificationService, times(2))
                    .send(any(User.class), eq(NotificationType.SYSTEM), eq("Dispute resolved"), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when admin email not found")
        void resolve_userNotFound_throws() {
            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("NO_ACTION");
            req.setAdminNotes("No action");

            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.resolveDispute(300L, "admin@skillbridge.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dispute not found")
        void resolve_disputeNotFound_throws() {
            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("NO_ACTION");
            req.setAdminNotes("No action");

            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.of(admin));
            when(disputeRepository.findById(300L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.resolveDispute(300L, "admin@skillbridge.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dispute not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when non-admin tries to resolve")
        void resolve_nonAdmin_throws() {
            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("NO_ACTION");
            req.setAdminNotes("nope");

            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));

            assertThatThrownBy(() ->
                    disputeService.resolveDispute(300L, "client@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only admins");
        }

        @Test
        @DisplayName("should throw BadRequestException when resolving already-resolved dispute")
        void resolve_alreadyResolved_throws() {
            openDispute.setStatus(DisputeStatus.RESOLVED);

            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("SPLIT");
            req.setAdminNotes("Already done");

            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.of(admin));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));

            assertThatThrownBy(() ->
                    disputeService.resolveDispute(300L, "admin@skillbridge.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already resolved");
        }

        @Test
        @DisplayName("should resolve dispute with SPLIT resolution")
        void resolve_split_success() {
            DisputeResolveRequest req = new DisputeResolveRequest();
            req.setResolution("SPLIT");
            req.setAdminNotes("Split between both parties.");

            DisputeTicket savedTicket = new DisputeTicket();
            savedTicket.setId(300L);
            savedTicket.setProject(activeProject);
            savedTicket.setReporter(client);
            savedTicket.setRespondent(freelancer);
            savedTicket.setStatus(DisputeStatus.RESOLVED);
            savedTicket.setResolution(DisputeResolution.SPLIT);
            savedTicket.setAdminNotes("Split between both parties.");
            savedTicket.setResolvedBy(admin);
            savedTicket.setResolvedAt(Instant.now());

            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.of(admin));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));
            when(disputeRepository.save(any(DisputeTicket.class))).thenReturn(savedTicket);

            DisputeResponse response = disputeService.resolveDispute(300L, "admin@skillbridge.com", req);

            assertThat(response.getResolution()).isEqualTo("SPLIT");
            verify(notificationService, times(2))
                    .send(any(User.class), eq(NotificationType.SYSTEM), eq("Dispute resolved"), contains("split between both parties"), anyString());
        }
    }

    @Nested
    @DisplayName("getMyDisputes()")
    class GetMyDisputesTests {

        @Test
        @DisplayName("user can fetch own disputes")
        void getMyDisputes_success() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(disputeRepository.findByUserId(1L)).thenReturn(List.of(openDispute));

            List<DisputeResponse> result = disputeService.getMyDisputes("client@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("OPEN");
            assertThat(result.get(0).getProjectId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("should return empty list when user has no disputes")
        void getMyDisputes_emptyList() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(disputeRepository.findByUserId(1L)).thenReturn(List.of());

            List<DisputeResponse> result = disputeService.getMyDisputes("client@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void getMyDisputes_userNotFound_throws() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.getMyDisputes("client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getDispute()")
    class GetDisputeTests {

        @Test
        @DisplayName("participant can view dispute")
        void getDispute_participant_success() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));

            DisputeResponse response = disputeService.getDispute(300L, "client@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(300L);
            assertThat(response.getStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("respondent can view dispute")
        void getDispute_respondent_success() {
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));

            DisputeResponse response = disputeService.getDispute(300L, "freelancer@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(300L);
            assertThat(response.getRespondentId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("admin can view dispute")
        void getDispute_admin_success() {
            when(userRepository.findByEmail("admin@skillbridge.com")).thenReturn(Optional.of(admin));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));

            DisputeResponse response = disputeService.getDispute(300L, "admin@skillbridge.com");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when viewer not found")
        void getDispute_userNotFound_throws() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.getDispute(300L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when dispute not found")
        void getDispute_notFound_throws() {
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(disputeRepository.findById(300L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.getDispute(300L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Dispute not found");
        }

        @Test
        @DisplayName("non participant cannot view dispute")
        void getDispute_nonParticipant_throws() {
            User stranger = new User();
            stranger.setId(99L);
            stranger.setEmail("stranger@example.com");
            stranger.setName("Stranger");
            stranger.setRole(Role.CLIENT);

            when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));
            when(disputeRepository.findById(300L)).thenReturn(Optional.of(openDispute));

            assertThatThrownBy(() ->
                    disputeService.getDispute(300L, "stranger@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("do not have access");
        }
    }

    @Nested
    @DisplayName("getAllDisputes()")
    class GetAllDisputesTests {

        @Test
        @DisplayName("should return paged disputes for admin listing")
        void getAllDisputes_success() {
            DisputeTicket second = new DisputeTicket();
            second.setId(301L);
            second.setProject(activeProject);
            second.setReporter(freelancer);
            second.setRespondent(client);
            second.setStatus(DisputeStatus.UNDER_REVIEW);
            second.setReason("Client is unresponsive.");
            second.setDescription("The client has not responded for many days.");

            when(disputeRepository.findAllForAdmin(PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(openDispute, second)));

            List<DisputeResponse> result = disputeService.getAllDisputes(0, 10);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(300L);
            assertThat(result.get(1).getId()).isEqualTo(301L);
        }

        @Test
        @DisplayName("should parse csv urls into list in admin listing")
        void getAllDisputes_parsesUrls() {
            openDispute.setEvidenceUrls("https://a.com/1.png, https://a.com/2.png");
            openDispute.setRespondentEvidenceUrls("https://b.com/1.png");

            when(disputeRepository.findAllForAdmin(PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(openDispute)));

            List<DisputeResponse> result = disputeService.getAllDisputes(0, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEvidenceUrls())
                    .containsExactly("https://a.com/1.png", "https://a.com/2.png");
            assertThat(result.get(0).getRespondentEvidenceUrls())
                    .containsExactly("https://b.com/1.png");
        }
    }
}