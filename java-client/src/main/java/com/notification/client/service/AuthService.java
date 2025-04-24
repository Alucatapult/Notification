package com.notification.client.service;

import com.notification.client.model.AuthRequest;
import com.notification.client.model.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    @Value("${notification.server.url}")
    private String serverUrl;

    private final RestTemplate restTemplate;
    private String jwtToken;

    public AuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String authenticate(String username, String password) {
        try {
            String authUrl = serverUrl + "/api/authenticate";
            AuthRequest authRequest = new AuthRequest(username, password);
            
            log.info("Authenticating user: {}", username);
            AuthResponse response = restTemplate.postForObject(
                authUrl, 
                authRequest, 
                AuthResponse.class
            );
            
            if (response != null && response.getToken() != null) {
                this.jwtToken = response.getToken();
                log.info("Authentication successful for user: {}", username);
                return this.jwtToken;
            } else {
                log.error("Authentication failed for user: {}", username);
                return null;
            }
        } catch (Exception e) {
            log.error("Error during authentication: {}", e.getMessage());
            return null;
        }
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        return headers;
    }
} 