package com.notification.client.config;

import com.notification.client.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    @Value("${notification.server.ws-url}")
    private String serverWsUrl;

    private final AuthService authService;

    @Bean
    public WebSocketStompClient webSocketStompClient() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        
        WebSocketClient client = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        return stompClient;
    }

    public StompHeaders createConnectHeaders() {
        StompHeaders headers = new StompHeaders();
        String token = authService.getJwtToken();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public String getServerWsUrl() {
        return serverWsUrl;
    }
} 