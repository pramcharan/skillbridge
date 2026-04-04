package com.skillbridge.service;

import com.skillbridge.entity.Project;
import com.skillbridge.entity.enums.NotificationType;
import com.skillbridge.entity.enums.ProjectStatus;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobService          jobService;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final ProjectRepository projectRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService          notificationService;
    private final RoomPresenceRepository presenceRepository;
    private static final long ONLINE_MINS = 5;

    // ── Auto-expire jobs every hour ───────────────────────────────────
    @Scheduled(fixedRate = 3600000) // every 1 hour
    @Transactional
    public void expireOldJobs() {
        log.info("Scheduler: checking for expired jobs...");
        jobService.expireOldJobs();
    }

    // Runs every 2 minutes — marks users offline if no heartbeat
    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void cleanStalePresence() {
        Instant cutoff = Instant.now()
                .minus(ONLINE_MINS, ChronoUnit.MINUTES);
        presenceRepository.markStaleOffline(cutoff);
    }


    // ── Clean up old read notifications every day at 2am ─────────────
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanOldNotifications() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        notificationRepository.deleteOldReadNotifications(cutoff);
        log.info("Scheduler: cleaned notifications older than 30 days");
    }


    // ── Clean expired password reset tokens daily at midnight ─────────────
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanExpiredPasswordTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Scheduler: cleaned expired password reset tokens");
    }

    // ── Clean expired revoked JWT tokens daily at 1am ─────────────────────
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanExpiredRevokedTokens() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        revokedTokenRepository.deleteByRevokedAtBefore(cutoff);
        log.info("Scheduler: cleaned expired revoked JWT tokens");
    }

    // ── Alert inactive projects daily at 9am ──────────────────────────────
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void alertInactiveProjects() {
        Instant threshold = Instant.now().minus(3, ChronoUnit.DAYS);

        List<Project> activeProjects = projectRepository
                .findByStatus(ProjectStatus.ACTIVE);

        for (Project project : activeProjects) {
            try {
                Instant lastActivity = chatMessageRepository
                        .findLastMessageTime(project.getId())
                        .orElse(project.getCreatedAt());

                if (lastActivity.isBefore(threshold)) {
                    notificationService.send(
                            project.getClient(),
                            NotificationType.SYSTEM,
                            "Project inactive for 3+ days",
                            "Your project \"" + project.getJob().getTitle() +
                                    "\" has had no activity for 3 days.",
                            "/project.html?id=" + project.getId());

                    notificationService.send(
                            project.getFreelancer(),
                            NotificationType.SYSTEM,
                            "Project needs attention",
                            "Your project \"" + project.getJob().getTitle() +
                                    "\" has had no activity for 3 days.",
                            "/project.html?id=" + project.getId());

                    log.info("Scheduler: inactivity alert sent for project {}",
                            project.getId());
                }
            } catch (Exception e) {
                log.error("Scheduler: error checking project {}: {}",
                        project.getId(), e.getMessage());
            }
        }
    }
}