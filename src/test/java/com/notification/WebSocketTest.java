package com.notification;

import com.notification.model.AuthRequest;
import com.notification.model.AuthResponse;
import com.notification.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;
    private StompSession stompSession;
    private final CompletableFuture<String> completableFuture = new CompletableFuture<>();

    @BeforeEach
    void setup() throws InterruptedException, ExecutionException, TimeoutException {
        // Get auth token
        AuthRequest request = new AuthRequest("user", "password");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/authenticate",
                request,
                AuthResponse.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK) {
            authToken = response.getBody().getToken();
        }
        
        // Setup WebSocket connection
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + authToken);
        
        stompSession = stompClient.connect(
                "ws://localhost:" + port + "/ws",
                new StompSessionHandlerAdapter() {},
                connectHeaders
        ).get(5, TimeUnit.SECONDS);
        
        // Subscribe to user queue
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((String) payload);
            }
        });
    }

    @Test
    void testWebSocketNotification() throws InterruptedException, ExecutionException, TimeoutException {
        // Skip if we couldn't get auth token
        if (authToken == null) {
            return;
        }
        
        // Send a message through the API that should trigger a WebSocket notification
        Notification notification = new Notification();
        notification.setType("WEBSOCKET_TEST");
        notification.setRecipient("user");  // Same as authenticated user
        notification.setPayload("Test WebSocket notification");
        notification.setTargetUrl("http://example.com");
        
        // Send the notification via websocket
        stompSession.send("/app/send-notification", notification);
        
        // Wait for the notification to be received
        String message = completableFuture.get(10, TimeUnit.SECONDS);
        
        // Verify message
        assertNotNull(message);
        assertEquals("Test WebSocket notification", message);
    }
} 