package com.notification.repository;

import com.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find notifications for a specific recipient, ordered by creation time descending
     * 
     * @param recipient The recipient username
     * @return List of notifications
     */
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    
    /**
     * Find failed notifications that need retry
     * 
     * @return List of failed notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' OR n.status = 'PENDING_RETRY'")
    List<Notification> findFailedNotifications();
    
    /**
     * Find notifications older than a given date for cleanup
     * 
     * @param cutoffDate The cutoff date
     * @return List of old notifications
     */
    List<Notification> findByCreatedAtBefore(LocalDateTime cutoffDate);
}