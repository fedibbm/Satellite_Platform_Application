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
     * For image messages, this will be "📷 Image" or similar indicator.
     */
    private String lastMessagePreview;

    /**
     * Type of the last message (TEXT or IMAGE).
     * Used to display appropriate icon/preview in conversation list.
     */
    private MessageType lastMessageType;

    /**
     * ID of the user who sent the last message.
     * Useful for displaying "You:" prefix in UI.
     */
    private String lastMessageSenderId;

    /**
     * Count of unread messages for each participant.
     * Map structure: {encodedUserId -> unreadCount}
     * Note: User IDs with dots (like emails) are encoded by replacing '.' with '___DOT___'
     * Example: {"user1Id": 0, "user2___DOT___example___DOT___com": 3}
     */
    private java.util.Map<String, Integer> unreadCounts;

    /**
     * Encodes a user ID to be safe for MongoDB map keys (replaces dots).
     */
    private static String encodeUserId(String userId) {
        return userId == null ? null : userId.replace(".", "___DOT___");
    }

    /**
     * Decodes a user ID from MongoDB map key format.
     */
    private static String decodeUserId(String encodedUserId) {
        return encodedUserId == null ? null : encodedUserId.replace("___DOT___", ".");
    }

    /**
     * Updates conversation metadata when a new message is sent.
     * Should be called whenever a message is added to this conversation.
     * 
     * @param messagePreview Preview text (or "📷 Image" for image messages)
     * @param messageType Type of the message (TEXT or IMAGE)
     * @param senderId ID of the user who sent the message
     */
    public void updateLastMessage(String messagePreview, MessageType messageType, String senderId) {
        this.lastMessageAt = LocalDateTime.now();
        this.lastMessagePreview = messagePreview;
        this.lastMessageType = messageType;
        this.lastMessageSenderId = senderId;
    }

    /**
     * Updates conversation metadata when a new message is sent (text only).
     * Convenience method for backward compatibility.
     */
    public void updateLastMessage(String messagePreview, String senderId) {
        updateLastMessage(messagePreview, MessageType.TEXT, senderId);
    }

    /**
     * Increments unread count for a specific user.
     */
    public void incrementUnreadCount(String userId) {
        if (this.unreadCounts == null) {
            this.unreadCounts = new java.util.HashMap<>();
        }
        String encodedUserId = encodeUserId(userId);
        this.unreadCounts.put(encodedUserId, this.unreadCounts.getOrDefault(encodedUserId, 0) + 1);
    }

    /**
     * Resets unread count for a specific user to zero.
     * Called when user reads messages.
     */
    public void resetUnreadCount(String userId) {
        if (this.unreadCounts != null) {
            this.unreadCounts.put(encodeUserId(userId), 0);
        }
    }

    /**
     * Gets unread count for a specific user.
     */
    public int getUnreadCount(String userId) {
        if (this.unreadCounts == null) {
            return 0;
        }
        return this.unreadCounts.getOrDefault(encodeUserId(userId), 0);
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
