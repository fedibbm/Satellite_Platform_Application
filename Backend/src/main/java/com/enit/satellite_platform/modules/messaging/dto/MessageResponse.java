package com.enit.satellite_platform.modules.messaging.dto;

import com.enit.satellite_platform.modules.messaging.entities.MessageStatus;
import com.enit.satellite_platform.modules.messaging.entities.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a message with all its details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String id;
    private String conversationId;
    private String senderId;
    private String recipientId;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private MessageStatus status;
    
    // Optional: enriched sender info
    private String senderName;
    private String senderAvatar;
}
