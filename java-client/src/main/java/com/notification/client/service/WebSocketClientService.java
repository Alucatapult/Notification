package com.notification.client.service;

import com.notification.client.config.WebSocketConfig;
import com.notification.client.websocket.NotificationSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class WebSocketClientService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientService.class);
    
    private final WebSocketStompClient stompClient;
    private final WebSocketConfig webSocketConfig;
    private final NotificationSessionHandler sessionHandler;
    
    private StompSession stompSession;

    public WebSocketClientService(WebSocketStompClient stompClient, WebSocketConfig webSocketConfig, 
                                  NotificationSessionHandler sessionHandler) {
        this.stompClient = stompClient;
        this.webSocketConfig = webSocketConfig;
        this.sessionHandler = sessionHandler;
    }

    public boolean connect(String username) {
        try {
            if (stompSession != null && stompSession.isConnected()) {
                log.info("Already connected to WebSocket server");
                return true;
            }
            
            log.info("Connecting to WebSocket server at: {}", webSocketConfig.getServerWsUrl());
            
            // Convert StompHeaders to WebSocketHttpHeaders
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            String token = webSocketConfig.createConnectHeaders().getFirst("Authorization");
            if (token != null) {
                headers.add("Authorization", token);
            }
            
            stompSession = stompClient.connect(
                webSocketConfig.getServerWsUrl(), 
                headers,
                sessionHandler
            ).get(5, TimeUnit.SECONDS);
            
            if (stompSession.isConnected()) {
                log.info("Successfully connected to WebSocket server");
                sessionHandler.subscribeToUserQueue(stompSession, username);
                return true;
            } else {
                log.error("Failed to connect to WebSocket server");
                return false;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error connecting to WebSocket server: {}", e.getMessage());
            return false;
        }
    }
    
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("Disconnected from WebSocket server");
        }
    }
    
    public void addMessageHandler(Consumer<String> handler) {
        sessionHandler.addMessageHandler(handler);
    }
    
    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
} 