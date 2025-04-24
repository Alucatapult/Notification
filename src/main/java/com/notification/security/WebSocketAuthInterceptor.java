package com.notification.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                
                if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                    String jwtToken = bearerToken.substring(7);
                    
                    try {
                        String username = jwtTokenUtil.extractUsername(jwtToken);
                        
                        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            
                            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                                UsernamePasswordAuthenticationToken authToken = 
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails, 
                                        null,
                                        userDetails.getAuthorities()
                                    );
                                
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                                accessor.setUser(authToken);
                                log.debug("WebSocket connection authenticated for user: {}", username);
                            }
                        }
                    } catch (Exception e) {
                        log.error("WebSocket authentication error: {}", e.getMessage());
                    }
                }
            }
        }
        
        return message;
    }
} 