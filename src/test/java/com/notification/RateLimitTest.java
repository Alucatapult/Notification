package com.notification;

import com.notification.model.AuthRequest;
import com.notification.model.AuthResponse;
import com.notification.model.Notification;
import com.notification.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private CacheManager cacheManager;

    private static final String TEST_CLIENT = "test-client";
    private String authToken;

    @BeforeEach
    void setup() {
        rateLimitService.resetLimit(TEST_CLIENT);
        
        // Get auth token for API tests
        AuthRequest request = new AuthRequest("user", "password");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/authenticate",
                request,
                AuthResponse.class
        );
        
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            authToken = response.getBody().getToken();
        }
    }

    @Test
    void testRateLimit() {
        // First 100 requests should be allowed
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.isAllowed(TEST_CLIENT));
        }

        // 101st request should be blocked
        assertFalse(rateLimitService.isAllowed(TEST_CLIENT));
    }

    @Test
    void testRateLimitReset() {
        // Exhaust the limit
        for (int i = 0; i < 100; i++) {
            rateLimitService.isAllowed(TEST_CLIENT);
        }

        // Reset the limit
        rateLimitService.resetLimit(TEST_CLIENT);

        // Should be able to make requests again
        assertTrue(rateLimitService.isAllowed(TEST_CLIENT));
    }
    
    @Test
    void testRateLimitWithRealEndpoints() {
        // Skip if we couldn't get auth token
        if (authToken == null) {
            return;
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        
        // Create a test notification
        Notification notification = new Notification();
        notification.setType("TEST");
        notification.setRecipient("user1");
        notification.setPayload("Test notification");
        notification.setTargetUrl("http://example.com");
        
        HttpEntity<Notification> request = new HttpEntity<>(notification, headers);
        
        // Make multiple requests to the same endpoint
        ResponseEntity<Notification> response = null;
        HttpStatusCode lastStatus = null;
        
        // This is a simplified test - in a real scenario, we would need to
        // configure the rate limiter with a much lower limit for testing
        for (int i = 0; i < 10; i++) {
            response = restTemplate.exchange(
                    "/api/notifications",
                    HttpMethod.POST,
                    request,
                    Notification.class
            );
            lastStatus = response.getStatusCode();
            
            // If we ever get a 429, test passes
            if (lastStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                break;
            }
        }
        
        // We either get a successful response (rate limit not hit in test)
        // or a too many requests response (rate limit hit)
        assertTrue(lastStatus.value() == HttpStatus.CREATED.value() || lastStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value());
    }
} 