package com.notification.service.impl;

import com.notification.audit.AuditLogger;
import com.notification.exception.NotificationException;
import com.notification.metrics.NotificationMetrics;
import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMetrics notificationMetrics;
    private final AuditLogger auditLogger;
    private static final int MAX_RETRIES = 3;
    private static final String NOTIFICATION_NOT_FOUND = "Notification not found with id: %d";
    private static final String NOTIFICATION_SERVICE = "notificationService";

    @Override
    @Transactional
    @CacheEvict(value = "notifications", key = "#notification.id", condition = "#notification.id != null")
    public Notification saveNotification(Notification notification) {
        try {
            log.info("Saving notification for recipient: {}", notification.getRecipient());
            
            boolean isNew = notification.getId() == null;
            String action = isNew ? "CREATE" : "UPDATE";
            
            Notification savedNotification = notificationRepository.save(notification);
            
            // Audit logging
            Map<String, Object> details = new HashMap<>();
            details.put("recipient", notification.getRecipient());
            details.put("type", notification.getType());
            details.put("status", notification.getStatus());
            
            auditLogger.logNotificationAction(action, savedNotification.getId(), details);
            
            return savedNotification;
        } catch (Exception e) {
            log.error("Error saving notification: {}", e.getMessage());
            
            // Audit logging for failure
            Map<String, Object> details = new HashMap<>();
            details.put("recipient", notification.getRecipient());
            details.put("type", notification.getType());
            details.put("error", e.getMessage());
            
            auditLogger.logNotificationAction("SAVE_FAILED", notification.getId(), details);
            
            throw new NotificationException("Failed to save notification", e);
        }
    }

    @Override
    @CircuitBreaker(name = NOTIFICATION_SERVICE, fallbackMethod = "getNotificationFallback")
    @Cacheable(value = "notifications", key = "#id")
    public Notification getNotification(Long id) {
        log.debug("Fetching notification with id: {}", id);
        try {
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error(NOTIFICATION_NOT_FOUND, id);
                        return new NotificationException(String.format(NOTIFICATION_NOT_FOUND, id));
                    });
            
            // Audit logging
            Map<String, Object> details = new HashMap<>();
            details.put("recipient", notification.getRecipient());
            details.put("type", notification.getType());
            
            auditLogger.logNotificationAction("READ", id, details);
            
            return notification;
        } catch (Exception e) {
            // Audit logging for failure
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            
            auditLogger.logNotificationAction("READ_FAILED", id, details);
            
            throw e; // Rethrow to trigger circuit breaker fallback
        }
    }

    public Notification getNotificationFallback(Long id, Exception e) {
        log.warn("Circuit breaker triggered for getNotification. Creating fallback notification for id: {}", id);
        // Return a fallback notification
        Notification fallback = new Notification();
        fallback.setId(id);
        fallback.setType("FALLBACK");
        fallback.setRecipient("system");
        fallback.setPayload("Service temporarily unavailable");
        fallback.setStatus("FALLBACK");
        fallback.setCreatedAt(LocalDateTime.now());
        return fallback;
    }

    @Override
    @Transactional
    @CircuitBreaker(name = NOTIFICATION_SERVICE, fallbackMethod = "processNotificationFallback")
    @Retryable(maxAttempts = MAX_RETRIES, 
               backoff = @Backoff(delay = 1000, multiplier = 2),
               exclude = {NotificationException.class})
    public void processNotification(Notification notification) {
        try {
            log.info("Processing notification for recipient: {}", notification.getRecipient());
            long startTime = System.currentTimeMillis();
            
            // Save notification
            notification = saveNotification(notification);
            
            // Send notification via WebSocket
            messagingTemplate.convertAndSendToUser(
                notification.getRecipient(),
                "/queue/notifications",
                notification.getPayload()
            );
            
            // Update notification status
            notification.setStatus("DELIVERED");
            notification.setProcessedAt(LocalDateTime.now());
            saveNotification(notification);
            
            // Record metrics
            long processingTime = System.currentTimeMillis() - startTime;
            notificationMetrics.recordNotificationProcessed(
                notification.getType(),
                notification.getStatus(),
                processingTime,
                notification.getPayload().length()
            );
            
            log.info("Successfully processed notification with id: {}", notification.getId());
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage());
            handleNotificationFailure(notification, e);
            throw new NotificationException("Failed to process notification", e);
        }
    }

    public void processNotificationFallback(Notification notification, Exception e) {
        log.warn("Circuit breaker triggered for processNotification. Saving notification in PENDING state for id: {}", 
                notification.getId() != null ? notification.getId() : "new");
        
        try {
            notification.setStatus("PENDING");
            notification.setErrorMessage("Service temporarily unavailable, will retry later");
            notificationRepository.save(notification);
            
            // Record metrics for fallback
            notificationMetrics.recordNotificationProcessed(
                notification.getType(),
                "FALLBACK",
                0,
                notification.getPayload() != null ? notification.getPayload().length() : 0
            );
        } catch (Exception ex) {
            log.error("Failed to save fallback notification: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    @CircuitBreaker(name = NOTIFICATION_SERVICE, fallbackMethod = "retryFailedNotificationFallback")
    public void retryFailedNotification(Notification notification) {
        try {
            if (notification.getRetryCount() < MAX_RETRIES) {
                log.info("Retrying failed notification with id: {}", notification.getId());
                long startTime = System.currentTimeMillis();
                
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus("RETRYING");
                saveNotification(notification);
                processNotification(notification);
                
                // Record retry metrics
                long processingTime = System.currentTimeMillis() - startTime;
                notificationMetrics.recordNotificationProcessed(
                    notification.getType(),
                    "RETRIED",
                    processingTime,
                    notification.getPayload() != null ? notification.getPayload().length() : 0
                );
            } else {
                log.error("Max retry attempts reached for notification with id: {}", notification.getId());
                notification.setStatus("FAILED");
                notification.setErrorMessage("Max retry attempts reached");
                saveNotification(notification);
                
                // Record failure metrics
                notificationMetrics.recordNotificationProcessed(
                    notification.getType(),
                    "FAILED",
                    0,
                    notification.getPayload() != null ? notification.getPayload().length() : 0
                );
            }
        } catch (Exception e) {
            log.error("Error retrying notification: {}", e.getMessage());
            throw new NotificationException("Failed to retry notification", e);
        }
    }

    public void retryFailedNotificationFallback(Notification notification, Exception e) {
        log.warn("Circuit breaker triggered for retryFailedNotification for id: {}", notification.getId());
    }

    private void handleNotificationFailure(Notification notification, Exception e) {
        notification.setStatus("FAILED");
        notification.setErrorMessage(e.getMessage());
        notification.setRetryCount(notification.getRetryCount() + 1);
        saveNotification(notification);
        
        // Record failure metrics
        notificationMetrics.recordNotificationProcessed(
            notification.getType(),
            "FAILED",
            0,
            notification.getPayload() != null ? notification.getPayload().length() : 0
        );
    }
    
    @Scheduled(fixedRate = 3600000) // Every hour
    @CacheEvict(value = "notifications", allEntries = true)
    public void clearCache() {
        log.info("Clearing notifications cache");
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByRecipient(String recipient) {
        log.debug("Fetching notifications for recipient: {}", recipient);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }
    
    @PostConstruct
    public void initializeMetrics() {
        // Set up a gauge for active notifications
        notificationMetrics.initializeActiveNotificationsGauge(() -> 
            notificationRepository.countByStatus("PENDING"));
    }
} 