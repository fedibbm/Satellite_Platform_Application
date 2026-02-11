package com.enit.satellite_platform.modules.messaging.config;

import com.enit.satellite_platform.modules.messaging.websocket.WebSocketAuthInterceptor;
import com.enit.satellite_platform.modules.messaging.websocket.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time messaging.
 * Configures STOMP over WebSocket with JWT authentication via HTTP-only cookies.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

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
     * Handshake interceptor authenticates via HTTP-only cookies during upgrade.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://localhost:3000") // Allow frontend origin
                .addInterceptors(webSocketHandshakeInterceptor) // Authenticate during HTTP handshake
                .withSockJS();
    }

    /**
     * Configure client inbound channel with JWT authentication interceptor.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
