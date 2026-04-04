package com.skillbridge.service;

import com.skillbridge.dto.request.RevisionRequestDTO;
import com.skillbridge.dto.request.RevisionStatusUpdateDTO;
import com.skillbridge.dto.response.RevisionResponse;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.RevisionRequest;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.RevisionStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.RevisionRequestRepository;
import com.skillbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevisionService Unit Tests")
class RevisionServiceTest {

    @Mock
    private RevisionRequestRepository revisionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RevisionService revisionService;

    private User client;
    private User freelancer;
    private User stranger;
    private Project activeProject;
    private RevisionRequest revision;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setRole(Role.CLIENT);
        client.setName("Client User");
        client.setAvatarUrl("client.png");

        freelancer = new User();
        freelancer.setId(2L);
        freelancer.setEmail("freelancer@example.com");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setName("Freelancer User");
        freelancer.setAvatarUrl("freelancer.png");

        stranger = new User();
        stranger.setId(99L);
        stranger.setEmail("stranger@example.com");
        stranger.setRole(Role.CLIENT);
        stranger.setName("Stranger");

        activeProject = new Project();
        activeProject.setId(50L);
        activeProject.setTitle("Website Revamp");
        activeProject.setClient(client);
        activeProject.setFreelancer(freelancer);
        activeProject.setStatus(ProjectStatus.IN_PROGRESS);

        revision = RevisionRequest.builder()
                .id(400L)
                .project(activeProject)
                .requester(client)
                .title("Login Fix")
                .description("Please fix the login flow.")
                .status(RevisionStatus.OPEN)
                .build();
    }

    @Nested
    @DisplayName("createRevision()")
    class CreateRevisionTests {

        @Test
        @DisplayName("should create revision request for IN_PROGRESS project by client")
        void createRevision_client_success() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Login Fix",
                    "Please fix the login flow."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(revisionRepository.countByProjectIdAndStatus(50L, RevisionStatus.OPEN)).thenReturn(3L);
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> {
                RevisionRequest r = inv.getArgument(0);
                r.setId(400L);
                return r;
            });

            RevisionResponse resp = revisionService.createRevision(50L, req, "client@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.projectId()).isEqualTo(50L);
            assertThat(resp.title()).isEqualTo("Login Fix");
            assertThat(resp.description()).isEqualTo("Please fix the login flow.");
            assertThat(resp.status()).isEqualTo(RevisionStatus.OPEN);
            assertThat(resp.requesterName()).isEqualTo("Client User");
            assertThat(resp.requesterIsClient()).isTrue();

            ArgumentCaptor<RevisionRequest> captor = ArgumentCaptor.forClass(RevisionRequest.class);
            verify(revisionRepository).save(captor.capture());

            RevisionRequest saved = captor.getValue();
            assertThat(saved.getProject()).isEqualTo(activeProject);
            assertThat(saved.getRequester()).isEqualTo(client);
            assertThat(saved.getTitle()).isEqualTo("Login Fix");
            assertThat(saved.getDescription()).isEqualTo("Please fix the login flow.");
            assertThat(saved.getStatus()).isEqualTo(RevisionStatus.OPEN);

            verify(notificationService).send(
                    eq(freelancer),
                    eq(NotificationType.GENERAL),
                    eq("New Revision Request"),
                    contains("Login Fix"),
                    eq("/project.html?id=50")
            );
        }

        @Test
        @DisplayName("should create revision request for IN_PROGRESS project by freelancer")
        void createRevision_freelancer_success() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Need Clarification",
                    "Please clarify the dashboard changes."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(revisionRepository.countByProjectIdAndStatus(50L, RevisionStatus.OPEN)).thenReturn(2L);
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> {
                RevisionRequest r = inv.getArgument(0);
                r.setId(401L);
                return r;
            });

            RevisionResponse resp = revisionService.createRevision(50L, req, "freelancer@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.projectId()).isEqualTo(50L);
            assertThat(resp.title()).isEqualTo("Need Clarification");
            assertThat(resp.status()).isEqualTo(RevisionStatus.OPEN);
            assertThat(resp.requesterName()).isEqualTo("Freelancer User");
            assertThat(resp.requesterIsClient()).isFalse();

            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.GENERAL),
                    eq("New Revision Request"),
                    contains("Need Clarification"),
                    eq("/project.html?id=50")
            );
        }

        @Test
        @DisplayName("should throw RuntimeException when max open revisions reached")
        void createRevision_maxReached_throws() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "One more fix",
                    "Please fix one more issue."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(revisionRepository.countByProjectIdAndStatus(50L, RevisionStatus.OPEN)).thenReturn(10L);

            assertThatThrownBy(() ->
                    revisionService.createRevision(50L, req, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Too many open revisions on this project");
        }

        @Test
        @DisplayName("should throw RuntimeException for non-IN_PROGRESS project")
        void createRevision_wrongStatus_throws() {
            activeProject.setStatus(ProjectStatus.COMPLETED);

            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Too late",
                    "Project already completed."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));

            assertThatThrownBy(() ->
                    revisionService.createRevision(50L, req, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Revisions can only be raised on active projects");
        }

        @Test
        @DisplayName("should throw RuntimeException for non-participant")
        void createRevision_nonParticipant_throws() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Can I also?",
                    "I am not part of this project."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));

            assertThatThrownBy(() ->
                    revisionService.createRevision(50L, req, "stranger@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not a project participant");
        }

        @Test
        @DisplayName("should throw RuntimeException when project not found")
        void createRevision_projectNotFound_throws() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Missing Project",
                    "This project does not exist."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.createRevision(50L, req, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Project not found");
        }

        @Test
        @DisplayName("should throw RuntimeException when user not found")
        void createRevision_userNotFound_throws() {
            RevisionRequestDTO req = new RevisionRequestDTO(
                    "Unknown User",
                    "User not found case."
            );

            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.createRevision(50L, req, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("should allow other participant to mark revision IN_PROGRESS")
        void updateStatus_inProgress_success() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.IN_PROGRESS,
                    "Started working on it"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            RevisionResponse resp = revisionService.updateStatus(400L, dto, "freelancer@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.status()).isEqualTo(RevisionStatus.IN_PROGRESS);
            assertThat(resp.resolutionNote()).isEqualTo("Started working on it");
            assertThat(resp.resolvedByName()).isNull();

            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.GENERAL),
                    eq("Revision In_progress"),
                    contains("marked as in progress"),
                    eq("/project.html?id=50")
            );
        }

        @Test
        @DisplayName("should resolve revision and set resolved fields")
        void updateStatus_resolved_success() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.RESOLVED,
                    "Issue fixed successfully"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            RevisionResponse resp = revisionService.updateStatus(400L, dto, "freelancer@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.status()).isEqualTo(RevisionStatus.RESOLVED);
            assertThat(resp.resolutionNote()).isEqualTo("Issue fixed successfully");
            assertThat(resp.resolvedByName()).isEqualTo("Freelancer User");
            assertThat(resp.resolvedAt()).isNotNull();

            verify(revisionRepository).save(argThat(r ->
                    r.getStatus() == RevisionStatus.RESOLVED
                            && "Issue fixed successfully".equals(r.getResolutionNote())
                            && r.getResolvedBy().equals(freelancer)
                            && r.getResolvedAt() != null
            ));

            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.GENERAL),
                    eq("Revision Resolved"),
                    contains("marked as resolved"),
                    eq("/project.html?id=50")
            );
        }

        @Test
        @DisplayName("should reject revision and set resolved fields")
        void updateStatus_rejected_success() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.REJECTED,
                    "This request is out of scope"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            RevisionResponse resp = revisionService.updateStatus(400L, dto, "freelancer@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.status()).isEqualTo(RevisionStatus.REJECTED);
            assertThat(resp.resolutionNote()).isEqualTo("This request is out of scope");
            assertThat(resp.resolvedByName()).isEqualTo("Freelancer User");
            assertThat(resp.resolvedAt()).isNotNull();

            verify(notificationService).send(
                    eq(client),
                    eq(NotificationType.GENERAL),
                    eq("Revision Rejected"),
                    contains("marked as rejected"),
                    eq("/project.html?id=50")
            );
        }

        @Test
        @DisplayName("should throw RuntimeException when requester resolves own revision")
        void updateStatus_requesterCannotResolveOwnRevision_throws() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.RESOLVED,
                    "I resolved it myself"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));

            assertThatThrownBy(() ->
                    revisionService.updateStatus(400L, dto, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("You cannot resolve your own revision request");
        }

        @Test
        @DisplayName("should allow requester to move own revision to IN_PROGRESS")
        void updateStatus_requesterCanSetInProgress() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.IN_PROGRESS,
                    "Tracking internally"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(revisionRepository.save(any(RevisionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            RevisionResponse resp = revisionService.updateStatus(400L, dto, "client@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.status()).isEqualTo(RevisionStatus.IN_PROGRESS);
            assertThat(resp.resolutionNote()).isEqualTo("Tracking internally");
            assertThat(resp.resolvedByName()).isNull();
            assertThat(resp.resolvedAt()).isNull();
        }

        @Test
        @DisplayName("should throw RuntimeException when revision not found")
        void updateStatus_revisionNotFound_throws() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.RESOLVED,
                    "Done"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.updateStatus(400L, dto, "freelancer@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Revision not found");
        }

        @Test
        @DisplayName("should throw RuntimeException when updater user not found")
        void updateStatus_userNotFound_throws() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.RESOLVED,
                    "Done"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.updateStatus(400L, dto, "freelancer@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should throw RuntimeException for non-participant updater")
        void updateStatus_nonParticipant_throws() {
            RevisionStatusUpdateDTO dto = new RevisionStatusUpdateDTO(
                    RevisionStatus.RESOLVED,
                    "Done"
            );

            when(revisionRepository.findById(400L)).thenReturn(Optional.of(revision));
            when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));

            assertThatThrownBy(() ->
                    revisionService.updateStatus(400L, dto, "stranger@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorised");
        }
    }

    @Nested
    @DisplayName("getProjectRevisions()")
    class GetProjectRevisionsTests {

        @Test
        @DisplayName("should return revisions for client participant")
        void getProjectRevisions_client_success() {
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(revisionRepository.findByProjectId(50L)).thenReturn(List.of(revision));

            List<RevisionResponse> result =
                    revisionService.getProjectRevisions(50L, "client@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).projectId()).isEqualTo(50L);
            assertThat(result.get(0).title()).isEqualTo("Login Fix");
            assertThat(result.get(0).requesterName()).isEqualTo("Client User");
        }

        @Test
        @DisplayName("should return revisions for freelancer participant")
        void getProjectRevisions_freelancer_success() {
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("freelancer@example.com")).thenReturn(Optional.of(freelancer));
            when(revisionRepository.findByProjectId(50L)).thenReturn(List.of(revision));

            List<RevisionResponse> result =
                    revisionService.getProjectRevisions(50L, "freelancer@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).projectTitle()).isEqualTo("Website Revamp");
        }

        @Test
        @DisplayName("should return empty list when project has no revisions")
        void getProjectRevisions_empty_success() {
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
            when(revisionRepository.findByProjectId(50L)).thenReturn(List.of());

            List<RevisionResponse> result =
                    revisionService.getProjectRevisions(50L, "client@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw RuntimeException when project not found")
        void getProjectRevisions_projectNotFound_throws() {
            when(projectRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.getProjectRevisions(50L, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Project not found");
        }

        @Test
        @DisplayName("should throw RuntimeException when user not found")
        void getProjectRevisions_userNotFound_throws() {
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    revisionService.getProjectRevisions(50L, "client@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should throw RuntimeException for non-participant")
        void getProjectRevisions_nonParticipant_throws() {
            when(projectRepository.findById(50L)).thenReturn(Optional.of(activeProject));
            when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.of(stranger));

            assertThatThrownBy(() ->
                    revisionService.getProjectRevisions(50L, "stranger@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorised");
        }
    }
}