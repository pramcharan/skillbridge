package com.skillbridge.service;

import com.skillbridge.dto.request.PortfolioItemRequest;
import com.skillbridge.dto.response.PortfolioItemResponse;
import com.skillbridge.entity.PortfolioItem;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.PortfolioItemRepository;
import com.skillbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Unit Tests")
class PortfolioServiceTest {

    @Mock
    private PortfolioItemRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private User freelancer;
    private PortfolioItem item;

    @BeforeEach
    void setUp() {
        freelancer = new User();
        freelancer.setId(2L);
        freelancer.setEmail("freelancer@example.com");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setName("Ram");

        item = new PortfolioItem();
        item.setId(500L);
        item.setTitle("E-Commerce Platform");
        item.setDescription("Full stack app with Spring Boot + React");
        item.setProjectUrl("https://github.com/ram/ecommerce");
        item.setImageUrl("https://img.com/project.png");
        item.setTags("Java, Spring Boot, React");
        item.setCategory("WEB_DEVELOPMENT");
        item.setCreatedAt(Instant.now());
        item.setUser(freelancer);
    }

    private PortfolioItemRequest buildRequest() {
        PortfolioItemRequest req = new PortfolioItemRequest();
        req.setTitle("E-Commerce Platform");
        req.setDescription("Full stack app with Spring Boot + React");
        req.setProjectUrl("https://github.com/ram/ecommerce");
        req.setImageUrl("https://img.com/project.png");
        req.setTags("Java, Spring Boot, React");
        req.setCategory("WEB_DEVELOPMENT");
        return req;
    }

    @Nested
    @DisplayName("addItem()")
    class AddItemTests {

        @Test
        @DisplayName("should add portfolio item successfully")
        void addItem_success() {
            PortfolioItemRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(portfolioRepository.countByUserId(2L))
                    .thenReturn(0L);
            when(portfolioRepository.save(any(PortfolioItem.class)))
                    .thenReturn(item);

            PortfolioItemResponse resp = portfolioService.addItem("freelancer@example.com", req);

            assertThat(resp).isNotNull();
            assertThat(resp.getId()).isEqualTo(500L);
            assertThat(resp.getTitle()).isEqualTo("E-Commerce Platform");
            assertThat(resp.getDescription()).isEqualTo("Full stack app with Spring Boot + React");
            assertThat(resp.getProjectUrl()).isEqualTo("https://github.com/ram/ecommerce");
            assertThat(resp.getImageUrl()).isEqualTo("https://img.com/project.png");
            assertThat(resp.getCategory()).isEqualTo("WEB_DEVELOPMENT");
            assertThat(resp.getOwnerId()).isEqualTo(2L);
            assertThat(resp.getOwnerName()).isEqualTo("Ram");
            assertThat(resp.getTags()).containsExactly("Java", "Spring Boot", "React");

            verify(portfolioRepository).save(argThat(saved ->
                    saved.getUser().equals(freelancer) &&
                            "E-Commerce Platform".equals(saved.getTitle()) &&
                            "Full stack app with Spring Boot + React".equals(saved.getDescription()) &&
                            "https://github.com/ram/ecommerce".equals(saved.getProjectUrl()) &&
                            "https://img.com/project.png".equals(saved.getImageUrl()) &&
                            "Java, Spring Boot, React".equals(saved.getTags()) &&
                            "WEB_DEVELOPMENT".equals(saved.getCategory())
            ));
        }

        @Test
        @DisplayName("should throw when portfolio limit reached")
        void addItem_limitReached_throws() {
            PortfolioItemRequest req = buildRequest();

            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(portfolioRepository.countByUserId(2L))
                    .thenReturn(20L);

            assertThatThrownBy(() -> portfolioService.addItem("freelancer@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Portfolio limit reached (20 items max).");

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when user not found")
        void addItem_userNotFound_throws() {
            PortfolioItemRequest req = buildRequest();

            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.addItem("missing@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: missing@example.com");

            verify(portfolioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPortfolio()")
    class GetPortfolioTests {

        @Test
        @DisplayName("should return all portfolio items for user")
        void getPortfolio_success() {
            when(portfolioRepository.findByUserIdOrderByCreatedAtDesc(2L))
                    .thenReturn(List.of(item));

            List<PortfolioItemResponse> result = portfolioService.getPortfolio(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(500L);
            assertThat(result.get(0).getTitle()).isEqualTo("E-Commerce Platform");
            assertThat(result.get(0).getOwnerName()).isEqualTo("Ram");
            assertThat(result.get(0).getOwnerId()).isEqualTo(2L);
            assertThat(result.get(0).getTags()).containsExactly("Java", "Spring Boot", "React");
        }

        @Test
        @DisplayName("should return empty list when user has no portfolio items")
        void getPortfolio_empty() {
            when(portfolioRepository.findByUserIdOrderByCreatedAtDesc(2L))
                    .thenReturn(List.of());

            List<PortfolioItemResponse> result = portfolioService.getPortfolio(2L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMyPortfolio()")
    class GetMyPortfolioTests {

        @Test
        @DisplayName("should return my portfolio using email")
        void getMyPortfolio_success() {
            when(userRepository.findByEmail("freelancer@example.com"))
                    .thenReturn(Optional.of(freelancer));
            when(portfolioRepository.findByUserIdOrderByCreatedAtDesc(2L))
                    .thenReturn(List.of(item));

            List<PortfolioItemResponse> result =
                    portfolioService.getMyPortfolio("freelancer@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("E-Commerce Platform");
            assertThat(result.get(0).getOwnerId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should throw when my portfolio user not found")
        void getMyPortfolio_userNotFound_throws() {
            when(userRepository.findByEmail("missing@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.getMyPortfolio("missing@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found: missing@example.com");
        }
    }

    @Nested
    @DisplayName("updateItem()")
    class UpdateItemTests {

        @Test
        @DisplayName("should update item when owner requests")
        void updateItem_success() {
            PortfolioItemRequest req = new PortfolioItemRequest();
            req.setTitle("Updated Project");
            req.setDescription("Updated description");
            req.setProjectUrl("https://github.com/ram/updated");
            req.setImageUrl("https://img.com/updated.png");
            req.setTags("Docker, Kubernetes");
            req.setCategory("BACKEND");

            when(portfolioRepository.findById(500L)).thenReturn(Optional.of(item));
            when(portfolioRepository.save(any(PortfolioItem.class))).thenReturn(item);

            PortfolioItemResponse resp =
                    portfolioService.updateItem(500L, "freelancer@example.com", req);

            assertThat(resp).isNotNull();
            assertThat(item.getTitle()).isEqualTo("Updated Project");
            assertThat(item.getDescription()).isEqualTo("Updated description");
            assertThat(item.getProjectUrl()).isEqualTo("https://github.com/ram/updated");
            assertThat(item.getImageUrl()).isEqualTo("https://img.com/updated.png");
            assertThat(item.getTags()).isEqualTo("Docker, Kubernetes");
            assertThat(item.getCategory()).isEqualTo("BACKEND");

            verify(portfolioRepository).save(item);
        }

        @Test
        @DisplayName("should throw when non-owner tries to update")
        void updateItem_nonOwner_throws() {
            PortfolioItemRequest req = buildRequest();

            when(portfolioRepository.findById(500L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() ->
                    portfolioService.updateItem(500L, "other@example.com", req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You can only edit your own portfolio items.");

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when item not found for update")
        void updateItem_notFound_throws() {
            PortfolioItemRequest req = buildRequest();

            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    portfolioService.updateItem(999L, "freelancer@example.com", req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Portfolio item not found: 999");

            verify(portfolioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteItem()")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete item when owner requests")
        void deleteItem_owner_success() {
            when(portfolioRepository.findById(500L)).thenReturn(Optional.of(item));

            portfolioService.deleteItem(500L, "freelancer@example.com");

            verify(portfolioRepository).delete(item);
        }

        @Test
        @DisplayName("should throw BadRequestException when non-owner tries to delete")
        void deleteItem_nonOwner_throws() {
            when(portfolioRepository.findById(500L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> portfolioService.deleteItem(500L, "other@example.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("You can only edit your own portfolio items.");

            verify(portfolioRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for missing item")
        void deleteItem_notFound_throws() {
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.deleteItem(999L, "freelancer@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Portfolio item not found: 999");

            verify(portfolioRepository, never()).delete(any());
        }
    }
}