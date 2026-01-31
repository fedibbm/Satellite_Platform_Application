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

/**
 * Message entity representing a single message in a 1-to-1 conversation.
 * 
 * Design Decisions:
 * - Standalone document (not embedded) for scalability
 * - Indexed fields for efficient querying
 * - Compound indexes for common query patterns
 * - Supports both text and image messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
    // Index for fetching all messages in a conversation, sorted by time
    @CompoundIndex(name = "conversation_timestamp_idx", 
                   def = "{'conversationId': 1, 'sentAt': 1}"),
    
    // Index for finding unread messages for a specific user
    @CompoundIndex(name = "recipient_status_idx", 
                   def = "{'recipientId': 1, 'status': 1}"),
    
    // Index for sender's message history
    @CompoundIndex(name = "sender_conversation_idx", 
                   def = "{'senderId': 1, 'conversationId': 1, 'sentAt': -1}")
})
public class Message {

    @Id
    private String id;

    /**
     * Reference to the conversation this message belongs to.
     * Allows efficient querying of all messages in a conversation.
     */
    @Indexed
    private String conversationId;

    /**
     * User ID of the sender.
     */
    @Indexed
    private String senderId;

    /**
     * User ID of the recipient.
     */
    @Indexed
    private String recipientId;

    /**
     * Type of message content (TEXT or IMAGE).
     */
    private MessageType messageType;

    /**
     * Text content of the message.
     * Required for TEXT messages, optional for IMAGE messages (can be a caption).
     */
    private String content;

    /**
     * URL or file path to the image.
     * Only populated for IMAGE type messages.
     * Format: /uploads/messages/{conversationId}/{filename}
     */
    private String imageUrl;

    /**
     * Timestamp when the message was sent.
     */
    @Indexed
    private LocalDateTime sentAt;

    /**
     * Timestamp when the message was read by the recipient.
     * Null if the message hasn't been read yet.
     */
    private LocalDateTime readAt;

    /**
     * Current status of the message (SENT or READ).
     */
    private MessageStatus status;

    /**
     * Marks this message as read.
     * Updates the status and sets the readAt timestamp.
     */
    public void markAsRead() {
        this.status = MessageStatus.READ;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Checks if the message has been read.
     */
    public boolean isRead() {
        return this.status == MessageStatus.READ;
    }
}
