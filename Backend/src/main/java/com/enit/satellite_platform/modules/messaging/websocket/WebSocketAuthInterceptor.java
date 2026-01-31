package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil;
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

/**
 * WebSocket authentication interceptor for JWT-based authentication.
 * Validates JWT token from WebSocket CONNECT frame and sets Spring Security context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from Authorization header or query parameter
            String token = extractToken(accessor);
            
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);
                
                log.debug("WebSocket authenticated for user: {}", username);
            } else {
                log.warn("WebSocket authentication failed - invalid or missing token");
                throw new IllegalArgumentException("Invalid JWT token");
            }
        }
        
        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     * Supports both Authorization header and token query parameter.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first: "Bearer <token>"
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Fallback to token query parameter
        String token = accessor.getFirstNativeHeader("token");
        if (token != null) {
            return token;
        }
        
        return null;
    }
}
