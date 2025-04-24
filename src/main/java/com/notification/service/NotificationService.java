package com.notification.service;

import com.notification.model.Notification;
import java.util.List;

public interface NotificationService {
    
    /**
     * Save a new notification
     * 
     * @param notification The notification to save
     * @return The saved notification with ID
     */
    Notification saveNotification(Notification notification);
    
    /**
     * Get a notification by its ID
     * 
     * @param id The notification ID
     * @return The notification
     */
    Notification getNotification(Long id);
    
    /**
     * Get all notifications for a specific recipient
     * 
     * @param recipient The recipient username
     * @return List of notifications
     */
    List<Notification> getNotificationsForRecipient(String recipient);
    
    /**
     * Process a notification (deliver it)
     * 
     * @param id The notification ID
     * @return The updated notification
     */
    Notification processNotification(Long id);
    
    /**
     * Retry failed notifications
     * 
     * @param id The notification ID
     * @return The updated notification
     */
    Notification retryFailedNotification(Long id);
}