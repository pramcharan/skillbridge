package com.skillbridge.service;

import com.skillbridge.dto.request.SubmitReviewRequest;
import com.skillbridge.dto.response.ReviewResponse;
import com.skillbridge.dto.response.ReviewSummaryResponse;
import com.skillbridge.entity.Project;
import com.skillbridge.entity.Review;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.ProjectRepository;
import com.skillbridge.repository.ReviewRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private User client;
    private User freelancer;
    private Project completedProject;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setRole(Role.CLIENT);
        client.setName("Alice");

        freelancer = new User();
        freelancer.setId(2L);
        freelancer.setEmail("freelancer@example.com");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setName("Bob");

        completedProject = new Project();
        completedProject.setId(50L);
        completedProject.setTitle("REST API Project");
        completedProject.setClient(client);
        completedProject.setFreelancer(freelancer);
        completedProject.setStatus(ProjectStatus.COMPLETED);
    }

    @Nested
    @DisplayName("submitReview()")
    class SubmitReviewTests {

        @Test
        @DisplayName("client can review freelancer on completed project")
        void clientReviewsFreelancer_success() {
            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(5);
            req.setComment("Excellent work!");
            req.setProjectId(50L);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));
            when(reviewRepository.existsByProjectId(50L))
                    .thenReturn(false);
            when(reviewRepository.save(any(Review.class)))
                    .thenAnswer(inv -> {
                        Review r = inv.getArgument(0);
                        r.setId(200L);
                        return r;
                    });
            when(reviewRepository.findAverageRatingByUserId(2L))
                    .thenReturn(Optional.of(5.0));
            when(reviewRepository.countByRevieweeId(2L))
                    .thenReturn(1L);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ReviewResponse resp = reviewService.submitReview(req, "client@example.com");

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(200L);
            assertThat(resp.getRating()).isEqualTo(5);
            assertThat(resp.getComment()).isEqualTo("Excellent work!");
            assertThat(resp.getProjectId()).isEqualTo(50L);
            assertThat(resp.getProjectTitle()).isEqualTo("REST API Project");
            assertThat(resp.getReviewerId()).isEqualTo(1L);
            assertThat(resp.getReviewerName()).isEqualTo("Alice");
            assertThat(resp.getRevieweeId()).isEqualTo(2L);
            assertThat(resp.getRevieweeName()).isEqualTo("Bob");

            verify(reviewRepository).save(any(Review.class));
            verify(userRepository).save(argThat(u ->
                    u.getId().equals(2L)
                            && u.getAvgRating() == 5.0
                            && u.getReviewCount() == 1
            ));
            verify(notificationService).send(
                    eq(freelancer),
                    eq(NotificationType.SYSTEM),
                    contains("New review from Alice"),
                    contains("Alice gave you 5 stars"),
                    eq("/profile.html")
            );
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when reviewer not found")
        void reviewerNotFound_throws() {
            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(5);
            req.setComment("Excellent work!");
            req.setProjectId(50L);

            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reviewService.submitReview(req, "missing@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: missing@example.com");

            verify(projectRepository, never()).findByIdWithDetails(anyLong());
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when project not found")
        void projectNotFound_throws() {
            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(5);
            req.setComment("Excellent work!");
            req.setProjectId(999L);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reviewService.submitReview(req, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project not found: 999");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when freelancer submits review")
        void freelancerCannotSubmitReview_throws() {
            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(4);
            req.setComment("Great client");
            req.setProjectId(50L);

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));

            assertThatThrownBy(() ->
                    reviewService.submitReview(req, "freelancer@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only the client can submit a review.");
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-client non-participant submits review")
        void nonParticipant_throws() {
            User stranger = new User();
            stranger.setId(99L);
            stranger.setEmail("stranger@example.com");
            stranger.setRole(Role.FREELANCER);

            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(3);
            req.setComment("Meh");
            req.setProjectId(50L);

            when(userRepository.findByEmail("stranger@example.com"))
                    .thenReturn(Optional.of(stranger));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));

            assertThatThrownBy(() -> reviewService.submitReview(req, "stranger@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only the client can submit a review.");
        }

        @Test
        @DisplayName("should throw BadRequestException on duplicate review")
        void duplicateReview_throws() {
            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(5);
            req.setComment("Again!");
            req.setProjectId(50L);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));
            when(reviewRepository.existsByProjectId(50L))
                    .thenReturn(true);

            assertThatThrownBy(() -> reviewService.submitReview(req, "client@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You have already reviewed this project.");
        }

        @Test
        @DisplayName("should throw BadRequestException for non-completed project")
        void nonCompletedProject_throws() {
            completedProject.setStatus(ProjectStatus.ACTIVE);

            SubmitReviewRequest req = new SubmitReviewRequest();
            req.setRating(4);
            req.setComment("Too early");
            req.setProjectId(50L);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));

            assertThatThrownBy(() -> reviewService.submitReview(req, "client@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You can only review completed projects.");
        }
    }

    @Nested
    @DisplayName("getReviewsForUser()")
    class GetReviewsTests {

        @Test
        @DisplayName("should return paged reviews for user")
        void getReviews_success() {
            Review review = new Review();
            review.setId(200L);
            review.setRating(5);
            review.setComment("Excellent!");
            review.setReviewee(freelancer);
            review.setReviewer(client);
            review.setProject(completedProject);

            Page<Review> reviewPage = new PageImpl<>(List.of(review));

            when(reviewRepository.findByRevieweeId(eq(2L), any(Pageable.class)))
                    .thenReturn(reviewPage);

            Page<ReviewResponse> result =
                    reviewService.getReviewsForUser(2L, Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
            assertThat(result.getContent().get(0).getReviewerName()).isEqualTo("Alice");
            assertThat(result.getContent().get(0).getRevieweeName()).isEqualTo("Bob");
            assertThat(result.getContent().get(0).getProjectTitle()).isEqualTo("REST API Project");
        }

        @Test
        @DisplayName("should return empty page when user has no reviews")
        void getReviews_emptyList() {
            when(reviewRepository.findByRevieweeId(eq(2L), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<ReviewResponse> result =
                    reviewService.getReviewsForUser(2L, Pageable.unpaged());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getReviewSummary()")
    class ReviewSummaryTests {

        @Test
        @DisplayName("should return correct summary for user with reviews")
        void summary_calculated() {
            Review r1 = new Review();
            r1.setRating(5);

            Review r2 = new Review();
            r2.setRating(4);

            Page<Review> page = new PageImpl<>(List.of(r1, r2));

            when(reviewRepository.findByRevieweeId(eq(2L), any(Pageable.class)))
                    .thenReturn(page);
            when(reviewRepository.countByRevieweeId(2L))
                    .thenReturn(2L);
            when(reviewRepository.findAverageRatingByUserId(2L))
                    .thenReturn(Optional.of(4.5));

            ReviewSummaryResponse summary = reviewService.getReviewSummary(2L);

            assertThat(summary.getTotalReviews()).isEqualTo(2L);
            assertThat(summary.getAverageRating()).isEqualTo(4.5);
            assertThat(summary.getFiveStar()).isEqualTo(1);
            assertThat(summary.getFourStar()).isEqualTo(1);
            assertThat(summary.getThreeStar()).isZero();
            assertThat(summary.getTwoStar()).isZero();
            assertThat(summary.getOneStar()).isZero();
        }

        @Test
        @DisplayName("should round average rating to one decimal place")
        void summary_roundsAverage() {
            when(reviewRepository.findByRevieweeId(eq(2L), any(Pageable.class)))
                    .thenReturn(Page.empty());
            when(reviewRepository.countByRevieweeId(2L))
                    .thenReturn(3L);
            when(reviewRepository.findAverageRatingByUserId(2L))
                    .thenReturn(Optional.of(4.66));

            ReviewSummaryResponse summary = reviewService.getReviewSummary(2L);

            assertThat(summary.getTotalReviews()).isEqualTo(3L);
            assertThat(summary.getAverageRating()).isEqualTo(4.7);
        }

        @Test
        @DisplayName("should return zero average when user has no reviews")
        void summary_noReviews_returnsZero() {
            when(reviewRepository.findByRevieweeId(eq(2L), any(Pageable.class)))
                    .thenReturn(Page.empty());
            when(reviewRepository.countByRevieweeId(2L))
                    .thenReturn(0L);
            when(reviewRepository.findAverageRatingByUserId(2L))
                    .thenReturn(Optional.empty());

            ReviewSummaryResponse summary = reviewService.getReviewSummary(2L);

            assertThat(summary.getTotalReviews()).isZero();
            assertThat(summary.getAverageRating()).isZero();
            assertThat(summary.getFiveStar()).isZero();
            assertThat(summary.getFourStar()).isZero();
            assertThat(summary.getThreeStar()).isZero();
            assertThat(summary.getTwoStar()).isZero();
            assertThat(summary.getOneStar()).isZero();
        }
    }

    @Nested
    @DisplayName("getReviewForProject()")
    class GetReviewForProjectTests {

        @Test
        @DisplayName("client can view review for project")
        void getReviewForProject_client_success() {
            Review review = new Review();
            review.setId(200L);
            review.setRating(5);
            review.setComment("Excellent!");
            review.setProject(completedProject);
            review.setReviewer(client);
            review.setReviewee(freelancer);

            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));
            when(reviewRepository.findByProjectId(50L))
                    .thenReturn(Optional.of(review));

            ReviewResponse result =
                    reviewService.getReviewForProject(50L, "client@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getReviewerName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("freelancer can view review for project")
        void getReviewForProject_freelancer_success() {
            Review review = new Review();
            review.setId(200L);
            review.setRating(5);
            review.setComment("Excellent!");
            review.setProject(completedProject);
            review.setReviewer(client);
            review.setReviewee(freelancer);

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));
            when(reviewRepository.findByProjectId(50L))
                    .thenReturn(Optional.of(review));

            ReviewResponse result =
                    reviewService.getReviewForProject(50L, "freelancer@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getRevieweeName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("should return null when review does not exist for authorized user")
        void getReviewForProject_noReview_returnsNull() {
            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));
            when(reviewRepository.findByProjectId(50L))
                    .thenReturn(Optional.empty());

            ReviewResponse result =
                    reviewService.getReviewForProject(50L, "client@example.com");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user missing")
        void getReviewForProject_userMissing_throws() {
            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reviewService.getReviewForProject(50L, "missing@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: missing@example.com");

            verify(projectRepository, never()).findByIdWithDetails(anyLong());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when unrelated user tries to view review")
        void getReviewForProject_nonParticipant_throws() {
            User stranger = new User();
            stranger.setId(99L);
            stranger.setEmail("stranger@example.com");

            when(userRepository.findByEmail("stranger@example.com"))
                    .thenReturn(Optional.of(stranger));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.of(completedProject));

            assertThatThrownBy(() ->
                    reviewService.getReviewForProject(50L, "stranger@example.com"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied.");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when project missing")
        void getReviewForProject_projectMissing_throws() {
            when(userRepository.findByEmail("client@example.com"))
                    .thenReturn(Optional.of(client));
            when(projectRepository.findByIdWithDetails(50L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    reviewService.getReviewForProject(50L, "client@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project not found: 50");
        }
    }
}