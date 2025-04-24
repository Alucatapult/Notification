package com.notification;

import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
class CircuitBreakerTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    void testCircuitBreakerFallback() {
        // Configure mock to throw exception
        when(notificationRepository.findById(anyLong()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Call service method with circuit breaker
        Notification notification = notificationService.getNotification(1L);

        // Verify fallback is returned
        assertNotNull(notification);
        assertEquals("FALLBACK", notification.getType());
        assertEquals("FALLBACK", notification.getStatus());
        assertEquals("system", notification.getRecipient());
        assertEquals("Service temporarily unavailable", notification.getPayload());
    }

    @Test
    void testCircuitBreakerNormalOperation() {
        // Configure mock to return normal value
        Notification mockNotification = new Notification();
        mockNotification.setId(1L);
        mockNotification.setType("TEST");
        mockNotification.setRecipient("user1");
        mockNotification.setPayload("Test notification");
        mockNotification.setStatus("PENDING");

        when(notificationRepository.findById(1L))
            .thenReturn(Optional.of(mockNotification));

        // Call service method with circuit breaker
        Notification notification = notificationService.getNotification(1L);

        // Verify normal response
        assertNotNull(notification);
        assertEquals("TEST", notification.getType());
        assertEquals("PENDING", notification.getStatus());
        assertEquals("user1", notification.getRecipient());
    }
} 