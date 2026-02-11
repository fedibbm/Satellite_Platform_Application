package com.enit.satellite_platform.modules.messaging.websocket;

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
import org.springframework.stereotype.Component;

/**
 * WebSocket channel interceptor that retrieves authentication from session attributes.
 * Authentication is established during handshake by WebSocketHandshakeInterceptor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTH_USER_KEY = "AUTH_USER";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Retrieve authentication from session attributes (set during handshake)
            UsernamePasswordAuthenticationToken authentication = 
                (UsernamePasswordAuthenticationToken) accessor.getSessionAttributes().get(AUTH_USER_KEY);
            
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);
                log.info("✅ STOMP session authenticated for user: {}", authentication.getName());
            } else {
                log.warn("❌ STOMP authentication failed - no user in session attributes");
                throw new IllegalArgumentException("Authentication required");
            }
        }
        
        return message;
    }
}
