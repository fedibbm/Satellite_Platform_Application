package com.enit.satellite_platform.modules.messaging.websocket;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Creates a WebSocket Principal from the cookie-based JWT auth used by HTTP requests.
 */
@Component
public class WebSocketCookieHandshakeHandler extends DefaultHandshakeHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketCookieHandshakeHandler.class);

    private final UserDetailsService userDetailsService;
    private final com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil jwtUtil;

    public WebSocketCookieHandshakeHandler(
            UserDetailsService userDetailsService,
            com.enit.satellite_platform.modules.user_management.management_cvore_service.security.Jwt.JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();

                // Prefer the already-authenticated HTTP principal.
                Principal requestPrincipal = httpRequest.getUserPrincipal();
                if (requestPrincipal != null) {
                    return requestPrincipal;
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    return authentication;
                }

                String token = null;
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("accessToken".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }

                if (token != null && jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("WebSocket handshake authenticated for {}", username);
                    return auth;
                }
            }
        } catch (Exception e) {
            log.warn("Unable to derive WebSocket user from cookie-authenticated request", e);
        }

        return super.determineUser(request, wsHandler, attributes);
    }
}
