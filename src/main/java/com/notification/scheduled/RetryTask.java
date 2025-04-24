package com.notification.scheduled;

import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RetryTask {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void retryFailedNotifications() {
        log.info("Starting retry of failed notifications");
        
        List<Notification> failedNotifications = notificationRepository.findFailedNotificationsToRetry();
        
        if (!failedNotifications.isEmpty()) {
            log.info("Found {} failed notifications to retry", failedNotifications.size());
            
            for (Notification notification : failedNotifications) {
                try {
                    notificationService.retryFailedNotification(notification);
                } catch (Exception e) {
                    log.error("Error retrying notification {}: {}", notification.getId(), e.getMessage());
                }
            }
        } else {
            log.info("No failed notifications to retry");
        }
    }
} 