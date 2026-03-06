package com.skillbridge.service;

import com.skillbridge.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobService          jobService;
    private final NotificationRepository notificationRepository;

    // ── Auto-expire jobs every hour ───────────────────────────────────
    @Scheduled(fixedRate = 3600000) // every 1 hour
    @Transactional
    public void expireOldJobs() {
        log.info("Scheduler: checking for expired jobs...");
        jobService.expireOldJobs();
    }

    // ── Clean up old read notifications every day at 2am ─────────────
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanOldNotifications() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        notificationRepository.deleteOldReadNotifications(cutoff);
        log.info("Scheduler: cleaned notifications older than 30 days");
    }
}