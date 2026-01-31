package com.enit.satellite_platform.modules.messaging.dto;

import com.enit.satellite_platform.modules.messaging.entities.Message;
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
    
    /**
     * Convert Message entity to MessageResponse DTO.
     */
    public static MessageResponse fromEntity(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .status(message.getStatus())
                .build();
    }
}
