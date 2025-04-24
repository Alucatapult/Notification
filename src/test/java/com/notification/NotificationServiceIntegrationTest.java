package com.notification;

import com.notification.model.Notification;
import com.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class NotificationServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    private HttpHeaders headers;

    @BeforeEach
    void setup() {
        headers = new HttpHeaders();
        String auth = "user:password";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("notification_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static RedisContainer redis = new RedisContainer("redis:7.0.0")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
            .withExposedPorts(5672, 15672)
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Test
    void testCreateAndGetNotification() {
        // Create notification
        Notification notification = new Notification();
        notification.setType("TEST");
        notification.setRecipient("user1");
        notification.setPayload("Test notification");
        notification.setTargetUrl("http://example.com");

        HttpEntity<Notification> request = new HttpEntity<>(notification, headers);
        ResponseEntity<Notification> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/notifications",
                HttpMethod.POST,
                request,
                Notification.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());

        // Get notification
        HttpEntity<String> getRequest = new HttpEntity<>(headers);
        ResponseEntity<Notification> getResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/notifications/" + response.getBody().getId(),
                HttpMethod.GET,
                getRequest,
                Notification.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals("TEST", getResponse.getBody().getType());
        assertEquals("user1", getResponse.getBody().getRecipient());
    }

    @Test
    void testNotificationPersistence() {
        Notification notification = new Notification();
        notification.setType("PERSISTENCE_TEST");
        notification.setRecipient("user2");
        notification.setPayload("Persistence test");
        notification.setTargetUrl("http://example.com");

        notification = notificationRepository.save(notification);
        assertNotNull(notification.getId());

        Notification found = notificationRepository.findById(notification.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("PERSISTENCE_TEST", found.getType());
        assertEquals("user2", found.getRecipient());
    }

    @Test
    void testUnauthorizedAccess() {
        HttpHeaders unauthorizedHeaders = new HttpHeaders();
        HttpEntity<Notification> request = new HttpEntity<>(new Notification(), unauthorizedHeaders);
        
        ResponseEntity<Notification> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/notifications",
                HttpMethod.POST,
                request,
                Notification.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
} 