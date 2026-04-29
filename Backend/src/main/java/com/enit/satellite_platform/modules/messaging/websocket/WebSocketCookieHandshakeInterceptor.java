package com.enit.satellite_platform.modules.messaging.websocket;

import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Captures authentication token from HTTP cookies during WebSocket handshake
 * and stores it in WebSocket session attributes for STOMP CONNECT authentication.
 */
@Component
public class WebSocketCookieHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketCookieHandshakeInterceptor.class);

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String SESSION_ATTR_ACCESS_TOKEN = "accessToken";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                            attributes.put(SESSION_ATTR_ACCESS_TOKEN, cookie.getValue());
                            log.debug("Captured accessToken cookie during WebSocket handshake");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read cookies during WebSocket handshake", e);
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
