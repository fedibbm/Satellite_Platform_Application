package com.enit.satellite_platform.modules.messaging.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for typing indicator events in real-time chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicator {
    private String senderId;
    private String recipientId;
    private boolean typing; // true when typing, false when stopped
    private Long timestamp;
}
