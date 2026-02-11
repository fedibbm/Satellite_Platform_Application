package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor that authenticates users via HTTP-only cookies.
 * Runs during the HTTP upgrade request before the WebSocket connection is established.
 * Extracts JWT from accessToken cookie, validates it, and stores authentication in session.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String AUTH_USER_KEY = "AUTH_USER";
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        
        log.debug("WebSocket handshake - extracting authentication from cookies");
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Extract token from cookies
            String token = extractTokenFromCookies(httpRequest);
            
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                // Store authentication in WebSocket session attributes
                attributes.put(AUTH_USER_KEY, authentication);
                
                log.info("✅ WebSocket handshake authenticated for user: {}", username);
                return true;
            } else {
                log.warn("❌ WebSocket handshake failed - invalid or missing JWT token");
                return false;
            }
        }
        
        log.warn("❌ WebSocket handshake failed - not a servlet request");
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Nothing to do after handshake
    }

    /**
     * Extract JWT token from HTTP cookies.
     */
    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    log.debug("Found accessToken cookie: {}...", token.substring(0, Math.min(20, token.length())));
                    return token;
                }
            }
        }
        log.debug("No accessToken cookie found in request");
        return null;
    }
}
