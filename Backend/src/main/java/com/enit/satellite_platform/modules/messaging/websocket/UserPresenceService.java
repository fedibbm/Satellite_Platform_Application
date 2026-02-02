package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track user online/offline presence via WebSocket connections.
 * Broadcasts presence changes to connected clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    
    // Map of userId (ObjectId) -> set of session IDs (a user can have multiple sessions/devices)
    private final Map<String, ConcurrentHashMap<String, Boolean>> userSessions = new ConcurrentHashMap<>();

    /**
     * Handle user connection event.
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();
        
        if (user != null && sessionId != null) {
            String email = user.getName(); // Email from JWT
            String userId = getUserIdFromEmail(email);
            
            if (userId != null) {
                userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(sessionId, true);
                
                log.info("User connected: {} (email: {}, session: {})", userId, email, sessionId);
                broadcastUserStatus(userId, true);
            } else {
                log.warn("Could not resolve email to userId: {}", email);
            }
        }
    }

    /**
     * Handle user disconnection event.
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();
        
        if (user != null && sessionId != null) {
            String email = user.getName(); // Email from JWT
            String userId = getUserIdFromEmail(email);
            
            if (userId != null) {
                ConcurrentHashMap<String, Boolean> sessions = userSessions.get(userId);
                
                if (sessions != null) {
                    sessions.remove(sessionId);
                    
                    // If user has no more active sessions, mark as offline
                    if (sessions.isEmpty()) {
                        userSessions.remove(userId);
                        log.info("User disconnected: {} (email: {}, last session: {})", userId, email, sessionId);
                        broadcastUserStatus(userId, false);
                    } else {
                        log.info("User session closed: {} (email: {}, session: {}), {} sessions remaining", 
                                 userId, email, sessionId, sessions.size());
                    }
                }
            }
        }
    }

    /**
     * Check if a user is currently online.
     */
    public boolean isUserOnline(String userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    /**
     * Broadcast user online/offline status to all connected clients.
     */
    private void broadcastUserStatus(String userId, boolean online) {
        Map<String, Object> status = Map.of(
            "userId", userId,
            "online", online,
            "timestamp", System.currentTimeMillis()
        );
        
        // Broadcast to /topic/presence so all clients can update their UI
        messagingTemplate.convertAndSend("/topic/presence", status);
        
        log.debug("Broadcasted presence: {} - {}", userId, online ? "ONLINE" : "OFFLINE");
    }

    /**
     * Get all online users (for debugging/admin purposes).
     */
    public Map<String, Integer> getOnlineUsers() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        userSessions.forEach((userId, sessions) -> result.put(userId, sessions.size()));
        return result;
    }

    /**
     * Convert email to ObjectId userId.
     */
    private String getUserIdFromEmail(String email) {
        try {
            return userRepository.findByEmail(email)
                    .map(user -> user.getId().toString())
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error resolving email to userId: {}", email, e);
            return null;
        }
    }
}
