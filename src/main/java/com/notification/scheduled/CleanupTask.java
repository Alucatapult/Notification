package com.notification.scheduled;

import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CleanupTask {

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM every day
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");
        
        // Get notifications older than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Notification> oldNotifications = notificationRepository.findOldNotifications(cutoffDate);
        
        if (!oldNotifications.isEmpty()) {
            log.info("Deleting {} old notifications", oldNotifications.size());
            notificationRepository.deleteAll(oldNotifications);
        } else {
            log.info("No old notifications to delete");
        }
    }
} 