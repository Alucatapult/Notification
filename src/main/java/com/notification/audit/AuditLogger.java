package com.notification.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogger {

    private static final String AUDIT_LOGGER_NAME = "AUDIT_LOG";
    private static final org.slf4j.Logger auditLogger = org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER_NAME);
    private final ObjectMapper objectMapper;
    
    /**
     * Log an audit event
     * 
     * @param action The action being performed
     * @param targetType The type of entity being acted upon
     * @param targetId The ID of the entity being acted upon
     * @param details Additional details about the action
     */
    public void logEvent(String action, String targetType, String targetId, Map<String, Object> details) {
        try {
            Map<String, Object> auditEvent = createAuditEvent(action, targetType, targetId, details);
            auditLogger.info(objectMapper.writeValueAsString(auditEvent));
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }
    
    /**
     * Log a notification action
     * 
     * @param action The action being performed (CREATE, READ, UPDATE, DELETE)
     * @param notificationId The ID of the notification
     * @param details Additional details about the notification
     */
    public void logNotificationAction(String action, Long notificationId, Map<String, Object> details) {
        logEvent(action, "NOTIFICATION", notificationId != null ? notificationId.toString() : "NEW", details);
    }
    
    /**
     * Log an authentication event
     * 
     * @param action The authentication action (LOGIN, LOGOUT, LOGIN_FAILED)
     * @param username The username attempting authentication
     * @param details Additional details about the authentication
     */
    public void logAuthEvent(String action, String username, Map<String, Object> details) {
        logEvent(action, "AUTH", username, details);
    }
    
    /**
     * Log a system event
     * 
     * @param action The system action
     * @param component The system component
     * @param details Additional details about the system event
     */
    public void logSystemEvent(String action, String component, Map<String, Object> details) {
        logEvent(action, "SYSTEM", component, details);
    }
    
    private Map<String, Object> createAuditEvent(String action, String targetType, String targetId, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventId", UUID.randomUUID().toString());
        auditEvent.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        auditEvent.put("username", username);
        auditEvent.put("action", action);
        auditEvent.put("targetType", targetType);
        auditEvent.put("targetId", targetId);
        
        if (details != null) {
            auditEvent.put("details", details);
        }
        
        return auditEvent;
    }
} 