package com.notification;

import com.notification.model.AuthRequest;
import com.notification.model.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSuccessfulAuthentication() {
        // Create auth request
        AuthRequest request = new AuthRequest("user", "password");
        
        // Perform authentication
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/authenticate",
                request,
                AuthResponse.class
        );
        
        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertTrue(response.getBody().getToken().length() > 0);
    }
    
    @Test
    void testFailedAuthentication() {
        // Create auth request with wrong password
        AuthRequest request = new AuthRequest("user", "wrongpassword");
        
        // Perform authentication
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/authenticate",
                request,
                AuthResponse.class
        );
        
        // Verify response
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    void testProtectedEndpointWithValidToken() {
        // First authenticate to get token
        AuthRequest request = new AuthRequest("user", "password");
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                "/api/authenticate",
                request,
                AuthResponse.class
        );
        
        // Create headers with token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.getBody().getToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        // Call protected endpoint
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/notifications/1",
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        
        // Either we get a 404 (if ID doesn't exist) or 200 (if it does)
        // But not 401/403
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND || 
                  response.getStatusCode() == HttpStatus.OK);
    }
    
    @Test
    void testProtectedEndpointWithoutToken() {
        // Call protected endpoint without token
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/notifications/1",
                String.class
        );
        
        // Should be unauthorized
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
} 