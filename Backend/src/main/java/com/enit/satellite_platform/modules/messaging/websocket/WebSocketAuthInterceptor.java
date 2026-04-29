package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * WebSocket authentication interceptor for JWT-based authentication.
 * Validates JWT token from WebSocket CONNECT frame and sets Spring Security context.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // If already authenticated during HTTP handshake, keep it.
            if (accessor.getUser() != null) {
                if (accessor.getUser() instanceof Authentication existingAuth && existingAuth.isAuthenticated()) {
                    SecurityContextHolder.getContext().setAuthentication(existingAuth);
                }
                return message;
            }

            // Extract JWT token from STOMP header, query param, or handshake cookie attributes.
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
     * Supports Authorization header, token query parameter, and handshake cookie attributes.
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

        // Fallback to token captured during HTTP handshake from cookies
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object cookieToken = sessionAttributes.get("accessToken");
            if (cookieToken instanceof String tokenFromCookie && !tokenFromCookie.isBlank()) {
                return tokenFromCookie;
            }
        }
        
        return null;
    }
}
