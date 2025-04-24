package com.notification.controller;

import com.notification.model.Notification;
import com.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationController(NotificationService notificationService, SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @Operation(summary = "Create a new notification", description = "Creates a new notification and delivers it via WebSocket if recipient is connected")
    public ResponseEntity<Notification> createNotification(@RequestBody Notification notification) {
        Notification saved = notificationService.saveNotification(notification);
        // Send via WebSocket to recipient
        messagingTemplate.convertAndSendToUser(
                notification.getRecipient(),
                "/queue/notifications",
                saved
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieves a notification by its ID")
    public ResponseEntity<Notification> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    @GetMapping("/recipient/{username}")
    @Operation(summary = "Get notifications for recipient", description = "Retrieves all notifications for a specific recipient")
    public ResponseEntity<List<Notification>> getNotificationsForRecipient(@PathVariable String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        
        // Security check: users can only access their own notifications
        if (!currentUsername.equals(username)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(notificationService.getNotificationsForRecipient(username));
    }
}