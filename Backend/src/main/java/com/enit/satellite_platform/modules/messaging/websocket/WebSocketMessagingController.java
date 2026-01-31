package com.enit.satellite_platform.modules.messaging.websocket;

import com.enit.satellite_platform.modules.messaging.dto.MessageResponse;
import com.enit.satellite_platform.modules.messaging.dto.SendMessageRequest;
import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.services.MessageService;
import com.enit.satellite_platform.modules.messaging.websocket.dto.TypingIndicator;
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

    /**
     * Handle real-time message sending via WebSocket.
     * Client sends to: /app/chat.send
     * Recipients receive at: /queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        try {
            String senderId = principal.getName();
            log.debug("WebSocket message from {}: {}", senderId, request.getContent());
            
            // Save message using existing service
            MessageResponse response = messageService.sendTextMessage(senderId, request.getRecipientId(), request.getContent());
            
            // Send to recipient's private queue
            messagingTemplate.convertAndSendToUser(
                request.getRecipientId(),
                "/queue/messages",
                response
            );
            
            // Also send back to sender for confirmation
            messagingTemplate.convertAndSendToUser(
                senderId,
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
        indicator.setSenderId(principal.getName());
        indicator.setTimestamp(System.currentTimeMillis());
        
        // Send typing indicator to recipient
        messagingTemplate.convertAndSendToUser(
            indicator.getRecipientId(),
            "/queue/typing",
            indicator
        );
        
        log.debug("Typing indicator: {} -> {} (typing: {})", 
                  indicator.getSenderId(), indicator.getRecipientId(), indicator.isTyping());
    }

    /**
     * Handle message read receipts.
     * Client sends to: /app/chat.read
     * Sender receives at: /queue/receipts
     */
    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload String messageId, Principal principal) {
        try {
            String userId = principal.getName();
            MessageResponse message = messageService.markAsRead(messageId, userId);
            
            // Notify the original sender that their message was read
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("messageId", messageId);
            receipt.put("readBy", userId);
            receipt.put("readAt", message.getReadAt());
            messagingTemplate.convertAndSendToUser(
                message.getSenderId(),
                "/queue/receipts",
                receipt
            );
            
            log.debug("Read receipt sent: message {} read by {}", messageId, userId);
            
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
