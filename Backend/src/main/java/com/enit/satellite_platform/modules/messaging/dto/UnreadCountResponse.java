package com.enit.satellite_platform.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for total unread message count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    /**
     * Total number of unread messages for the user.
     */
    private long totalUnreadCount;
    
    /**
     * Number of conversations with unread messages.
     */
    private long conversationsWithUnread;
}
