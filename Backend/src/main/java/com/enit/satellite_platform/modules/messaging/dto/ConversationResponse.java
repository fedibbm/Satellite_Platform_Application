package com.enit.satellite_platform.modules.messaging.dto;

import com.enit.satellite_platform.modules.messaging.entities.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a conversation with enriched user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private MessageType lastMessageType;
    private String lastMessageSenderId;
    private Integer unreadCount;
    
    // Enriched info about the other participant
    private String otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantAvatar;
    private Boolean otherParticipantOnline;
}
