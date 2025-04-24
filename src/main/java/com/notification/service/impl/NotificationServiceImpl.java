package com.notification.service.impl;

import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "notifications", allEntries = true)
    public Notification saveNotification(Notification notification) {
        logger.info("Saving notification for recipient: {}", notification.getRecipient());
        return notificationRepository.save(notification);
    }

    @Override
    @Cacheable(value = "notifications", key = "#id")
    public Notification getNotification(Long id) {
        logger.info("Fetching notification with ID: {}", id);
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with ID: " + id));
    }

    @Override
    @CircuitBreaker(name = "notificationService", fallbackMethod = "processNotificationFallback")
    @Transactional
    @CacheEvict(value = "notifications", key = "#id")
    public Notification processNotification(Long id) {
        logger.info("Processing notification with ID: {}", id);
        Notification notification = getNotification(id);
        
        try {
            // Simulate processing logic
            notification.setStatus("DELIVERED");
            notification.setDeliveredAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error processing notification: {}", e.getMessage());
            notification.setStatus("FAILED");
            return notificationRepository.save(notification);
        }
    }

    public Notification processNotificationFallback(Long id, Exception e) {
        logger.warn("Circuit breaker triggered for notification ID: {}. Error: {}", id, e.getMessage());
        Notification notification = getNotification(id);
        notification.setStatus("PENDING_RETRY");
        notification.incrementRetryCount();
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    @CacheEvict(value = "notifications", key = "#id")
    public Notification retryFailedNotification(Long id) {
        logger.info("Retrying failed notification with ID: {}", id);
        Notification notification = getNotification(id);
        
        if (!notification.getStatus().equals("FAILED") && !notification.getStatus().equals("PENDING_RETRY")) {
            throw new IllegalStateException("Can only retry failed notifications");
        }
        
        notification.setStatus("PENDING");
        notification.incrementRetryCount();
        notificationRepository.save(notification);
        
        return processNotification(id);
    }

    @Override
    public List<Notification> getNotificationsForRecipient(String recipient) {
        logger.info("Fetching notifications for recipient: {}", recipient);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }
}