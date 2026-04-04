package com.skillbridge.service;

import com.skillbridge.entity.Notification;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User client;

    @BeforeEach
    void setUp() {
        client = new User();
        client.setId(1L);
        client.setEmail("client@example.com");
        client.setName("Alice");
    }

    @Test
    @DisplayName("send should persist notification with correct fields")
    void send_persistsNotification() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(
                client,
                NotificationType.NEW_PROPOSAL,
                "New proposal received",
                "A freelancer applied to your job",
                "/proposals-client.html?jobId=10"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(NotificationType.NEW_PROPOSAL);
        assertThat(saved.getTitle()).isEqualTo("New proposal received");
        assertThat(saved.getMessage()).isEqualTo("A freelancer applied to your job");
        assertThat(saved.getLink()).isEqualTo("/proposals-client.html?jobId=10");
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("send should save unread notification")
    void send_setsUnreadFalse() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(
                client,
                NotificationType.PROPOSAL_UPDATE,
                "Proposal accepted!",
                "Your proposal was accepted.",
                "/dashboard-freelancer.html"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("send should not throw when repository save fails")
    void send_whenRepositoryFails_shouldNotThrow() {
        doThrow(new RuntimeException("DB error"))
                .when(notificationRepository).save(any(Notification.class));

        assertThatCode(() -> notificationService.send(
                client,
                NotificationType.PROPOSAL_UPDATE,
                "Title",
                "Message",
                "/link"
        )).doesNotThrowAnyException();

        verify(notificationRepository).save(any(Notification.class));
    }
}