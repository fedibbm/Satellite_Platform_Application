package com.enit.satellite_platform.modules.messaging.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Conversation entity representing a 1-to-1 chat between two users.
 * 
 * Design Decisions:
 * - Does NOT embed messages (messages are separate documents)
 * - Lightweight document that only stores metadata
 * - Participants stored as Set for easy lookup
 * - Tracks last message info for conversation list UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
@CompoundIndexes({
    // Index for finding a conversation between two specific users
    @CompoundIndex(name = "participants_idx", 
                   def = "{'participants': 1}", 
                   unique = true),
    
    // Index for sorting user's conversations by most recent activity
    @CompoundIndex(name = "participant_lastmessage_idx", 
                   def = "{'participants': 1, 'lastMessageAt': -1}")
})
public class Conversation {

    @Id
    private String id;

    /**
     * Set of exactly 2 user IDs participating in this conversation.
     * Using Set ensures order doesn't matter for lookup.
     * Example: ["user1Id", "user2Id"]
     */
    @Indexed
    private Set<String> participants;

    /**
     * Timestamp when the conversation was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent message in this conversation.
     * Used for sorting conversations in the UI.
     */
    @Indexed
    private LocalDateTime lastMessageAt;

    /**
     * Preview of the last message content (first 100 chars).
     * Cached here to avoid querying messages collection for conversation list.
     */
    private String lastMessagePreview;

    /**
     * ID of the user who sent the last message.
     * Useful for displaying "You:" prefix in UI.
     */
    private String lastMessageSenderId;

    /**
     * Count of unread messages for each participant.
     * Map structure: {userId -> unreadCount}
     * Example: {"user1Id": 0, "user2Id": 3}
     */
    private java.util.Map<String, Integer> unreadCounts;

    /**
     * Updates conversation metadata when a new message is sent.
     * Should be called whenever a message is added to this conversation.
     */
    public void updateLastMessage(String messagePreview, String senderId) {
        this.lastMessageAt = LocalDateTime.now();
        this.lastMessagePreview = messagePreview;
        this.lastMessageSenderId = senderId;
    }

    /**
     * Increments unread count for a specific user.
     */
    public void incrementUnreadCount(String userId) {
        if (this.unreadCounts == null) {
            this.unreadCounts = new java.util.HashMap<>();
        }
        this.unreadCounts.put(userId, this.unreadCounts.getOrDefault(userId, 0) + 1);
    }

    /**
     * Resets unread count for a specific user to zero.
     * Called when user reads messages.
     */
    public void resetUnreadCount(String userId) {
        if (this.unreadCounts != null) {
            this.unreadCounts.put(userId, 0);
        }
    }

    /**
     * Gets the other participant's ID in this 1-to-1 conversation.
     * 
     * @param currentUserId The current user's ID
     * @return The other participant's ID, or null if not found
     */
    public String getOtherParticipant(String currentUserId) {
        if (participants == null || participants.size() != 2) {
            return null;
        }
        return participants.stream()
            .filter(id -> !id.equals(currentUserId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if a user is a participant in this conversation.
     */
    public boolean hasParticipant(String userId) {
        return participants != null && participants.contains(userId);
    }
}
