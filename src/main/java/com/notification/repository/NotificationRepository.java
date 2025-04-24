package com.notification.repository;

import com.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    
    List<Notification> findByStatus(String status);
    
    long countByStatus(String status);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3")
    List<Notification> findFailedNotificationsToRetry();
    
    @Query("SELECT n FROM Notification n WHERE n.createdAt <= :cutoffDate AND n.status IN ('DELIVERED', 'FAILED')")
    List<Notification> findOldNotifications(LocalDateTime cutoffDate);
} 