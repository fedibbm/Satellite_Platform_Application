package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.messaging.dto.MessageResponse;
import com.enit.satellite_platform.modules.messaging.dto.SendMessageRequest;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.services.MessageService;
import com.enit.satellite_platform.modules.messaging.websocket.dto.TypingIndicator;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for real-time messaging features.
 * Handles incoming WebSocket messages and broadcasts updates to clients.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessagingController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;
    private final UserRepository userRepository;
    
    /**
     * Convert email to ObjectId
     */
    private String emailToObjectId(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
    
    /**
     * Convert ObjectId to email for WebSocket routing
     */
    private String objectIdToEmail(String objectId) {
        return userRepository.findById(new org.bson.types.ObjectId(objectId))
                .map(user -> user.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + objectId));
    }

    /**
     * Handle real-time message sending via WebSocket.
     * Client sends to: /app/chat.send
     * Recipients receive at: /queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        try {
            String senderEmail = principal.getName();
            String senderId = emailToObjectId(senderEmail); // Convert to ObjectId
            log.debug("WebSocket message from {} ({}): {}", senderEmail, senderId, request.getContent());
            
            // Save message using existing service
            MessageResponse response = messageService.sendTextMessage(senderId, request.getRecipientId(), request.getContent());
            
            // Convert ObjectIds to emails for WebSocket routing
            String recipientEmail = objectIdToEmail(request.getRecipientId());
            
            // Send to recipient's private queue (use email for routing)
            messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/messages",
                response
            );
            
            // Also send back to sender for confirmation (use email for routing)
            messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/messages",
                response
            );
            
            log.debug("Message delivered via WebSocket: {}", response.getId());
            
        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage(), e);
            // Send error to sender
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to send message");
            errorMap.put("message", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                errorMap
            );
        }
    }

    /**
     * Handle typing indicators.
     * Client sends to: /app/chat.typing
     * Recipient receives at: /queue/typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicator indicator, Principal principal) {
        String senderEmail = principal.getName();
        String senderId = emailToObjectId(senderEmail);  // Convert to ObjectId
        
        indicator.setSenderId(senderId);
        indicator.setTimestamp(System.currentTimeMillis());
        
        // Convert recipient ObjectId to email for WebSocket routing
        String recipientEmail = objectIdToEmail(indicator.getRecipientId());
        
        // Send typing indicator to recipient
        messagingTemplate.convertAndSendToUser(
            recipientEmail,  // Use email for routing
            "/queue/typing",
            indicator
        );
        
        log.debug("Typing indicator: {} ({}) -> {} (typing: {})", 
                  senderEmail, senderId, recipientEmail, indicator.isTyping());
    }

    /**
     * Handle message read receipts.
     * Client sends to: /app/chat.read
     * Sender receives at: /queue/receipts
     */
    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload String messageId, Principal principal) {
        try {
            String userEmail = principal.getName();
            String userId = emailToObjectId(userEmail); // Convert to ObjectId
            
            MessageResponse message = messageService.markAsRead(messageId, userId);
            
            // Convert sender ObjectId to email for WebSocket routing
            String senderEmail = objectIdToEmail(message.getSenderId());
            
            // Notify the original sender that their message was read
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("messageId", messageId);
            receipt.put("readBy", userId);
            receipt.put("readAt", message.getReadAt());
            messagingTemplate.convertAndSendToUser(
                senderEmail,  // Use email for routing
                "/queue/receipts",
                receipt
            );
            
            log.debug("Read receipt sent: message {} read by {} ({})", messageId, userEmail, userId);
            
        } catch (Exception e) {
            log.error("Error processing read receipt: {}", e.getMessage());
        }
    }

    /**
     * Get user online status.
     * Client sends to: /app/chat.status
     * Response to: /queue/status
     */
    @MessageMapping("/chat.status")
    public void checkUserStatus(@Payload String userId, Principal principal) {
        boolean online = userPresenceService.isUserOnline(userId);
        
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("userId", userId);
        statusMap.put("online", online);
        messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/status",
            statusMap
        );
    }
}
