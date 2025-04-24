package com.notification.client.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class NotificationSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(NotificationSessionHandler.class);
    private final List<Consumer<String>> messageHandlers = new ArrayList<>();

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Connected to WebSocket server");
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, 
                               byte[] payload, Throwable exception) {
        log.error("WebSocket error: {}", exception.getMessage(), exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", exception.getMessage(), exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return String.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (payload instanceof String) {
            String message = (String) payload;
            log.info("Received notification: {}", message);
            notifyHandlers(message);
        }
    }

    public void subscribeToUserQueue(StompSession session, String username) {
        String destination = "/user/" + username + "/queue/notifications";
        log.info("Subscribing to destination: {}", destination);
        session.subscribe(destination, this);
    }

    public void addMessageHandler(Consumer<String> handler) {
        messageHandlers.add(handler);
    }

    private void notifyHandlers(String message) {
        for (Consumer<String> handler : messageHandlers) {
            try {
                handler.accept(message);
            } catch (Exception e) {
                log.error("Error in message handler: {}", e.getMessage(), e);
            }
        }
    }
} 