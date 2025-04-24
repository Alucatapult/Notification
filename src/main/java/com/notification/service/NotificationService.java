package com.notification.service;

import com.notification.model.Notification;

import java.util.List;

public interface NotificationService {
    Notification saveNotification(Notification notification);
    Notification getNotification(Long id);
    void processNotification(Notification notification);
    void retryFailedNotification(Notification notification);
    List<Notification> getNotificationsByRecipient(String recipient);
} 