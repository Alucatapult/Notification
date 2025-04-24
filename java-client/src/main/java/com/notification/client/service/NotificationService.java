package com.notification.client.service;

import com.notification.client.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    @Value("${notification.server.url}")
    private String serverUrl;

    private final RestTemplate restTemplate;
    private final AuthService authService;

    public NotificationService(RestTemplate restTemplate, AuthService authService) {
        this.restTemplate = restTemplate;
        this.authService = authService;
    }

    public Notification createNotification(Notification notification) {
        try {
            String url = serverUrl + "/api/notifications";
            HttpEntity<Notification> requestEntity = new HttpEntity<>(notification, authService.createAuthHeaders());
            
            log.info("Creating notification for recipient: {}", notification.getRecipient());
            ResponseEntity<Notification> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Notification.class
            );
            
            log.info("Notification created with ID: {}", response.getBody() != null ? response.getBody().getId() : "unknown");
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage());
            return null;
        }
    }

    public Notification getNotification(Long id) {
        try {
            String url = serverUrl + "/api/notifications/" + id;
            HttpEntity<?> requestEntity = new HttpEntity<>(authService.createAuthHeaders());
            
            log.info("Fetching notification with ID: {}", id);
            ResponseEntity<Notification> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Notification.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching notification: {}", e.getMessage());
            return null;
        }
    }

    public List<Notification> getNotificationsForUser(String username) {
        try {
            String url = serverUrl + "/api/notifications/recipient/" + username;
            HttpEntity<?> requestEntity = new HttpEntity<>(authService.createAuthHeaders());
            
            log.info("Fetching notifications for user: {}", username);
            ResponseEntity<List<Notification>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Notification>>() {}
            );
            
            log.info("Fetched {} notifications for user: {}", 
                    response.getBody() != null ? response.getBody().size() : 0, username);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching notifications for user: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
} 