package com.notification.controller;

import com.notification.model.Notification;
import com.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Operation(summary = "Send notification via WebSocket", description = "Sends a notification to a specific user via WebSocket")
    @MessageMapping("/send-notification")
    public void sendNotification(@Payload Notification notification) {
        notificationService.processNotification(notification);
    }

    @Operation(summary = "Create a new notification", description = "Creates a new notification in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Notification created",
                    content = @Content(schema = @Schema(implementation = Notification.class))),
        @ApiResponse(responseCode = "400", description = "Invalid notification data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/api/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationService.saveNotification(notification);
    }

    @Operation(summary = "Get notification by ID", description = "Returns a notification by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification found",
                    content = @Content(schema = @Schema(implementation = Notification.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Notification not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @GetMapping("/api/notifications/{id}")
    public Notification getNotification(@PathVariable Long id) {
        return notificationService.getNotification(id);
    }

    @Operation(summary = "Get notifications by recipient", description = "Returns all notifications for a specific recipient")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications found",
                    content = @Content(schema = @Schema(implementation = Notification.class, type = "array"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @GetMapping("/api/notifications/recipient/{recipient}")
    public List<Notification> getNotificationsByRecipient(@PathVariable String recipient) {
        // Security check - user can only fetch their own notifications
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName().equals(recipient)) {
            return notificationService.getNotificationsByRecipient(recipient);
        } else {
            throw new AccessDeniedException("Cannot access notifications for other users");
        }
    }
} 