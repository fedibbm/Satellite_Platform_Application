package com.enit.satellite_platform.modules.messaging.config;

import com.enit.satellite_platform.modules.messaging.websocket.WebSocketAuthInterceptor;
import com.enit.satellite_platform.modules.messaging.websocket.WebSocketCookieHandshakeInterceptor;
import com.enit.satellite_platform.modules.messaging.websocket.WebSocketCookieHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time messaging.
 * Configures STOMP over WebSocket with JWT authentication.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketCookieHandshakeInterceptor webSocketCookieHandshakeInterceptor;
    private final WebSocketCookieHandshakeHandler webSocketCookieHandshakeHandler;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor,
                           WebSocketCookieHandshakeInterceptor webSocketCookieHandshakeInterceptor,
                           WebSocketCookieHandshakeHandler webSocketCookieHandshakeHandler) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.webSocketCookieHandshakeInterceptor = webSocketCookieHandshakeInterceptor;
        this.webSocketCookieHandshakeHandler = webSocketCookieHandshakeHandler;
    }

    /**
     * Configure message broker options.
     * - /topic: for broadcasting to multiple subscribers (public channels)
     * - /queue: for point-to-point messaging (private messages)
     * - /app: prefix for messages routed to @MessageMapping methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set prefix for messages destined for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connection.
     * Clients connect to: ws://localhost:8080/ws
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(webSocketCookieHandshakeHandler)
                .addInterceptors(webSocketCookieHandshakeInterceptor)
                .withSockJS()
                .setSessionCookieNeeded(true); // Ensure SockJS transports include cookies
    }

    /**
     * Configure client inbound channel with JWT authentication interceptor.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
