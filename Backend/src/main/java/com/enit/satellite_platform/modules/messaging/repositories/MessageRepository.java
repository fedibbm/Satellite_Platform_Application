package com.enit.satellite_platform.modules.messaging.repositories;

import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.entities.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity with pagination support.
 * 
 * Key features:
 * - Paginated message history queries
 * - Efficient unread message counting
 * - Conversation-based queries with sorting
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * Finds all messages in a conversation with pagination.
     * Results are sorted by sentAt (oldest first for natural chat flow).
     * 
     * Usage: For loading message history page by page
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters (page number, size, sort)
     * @return Page of messages
     */
    Page<Message> findByConversationIdOrderBySentAtAsc(String conversationId, Pageable pageable);

    /**
     * Finds all messages in a conversation with flexible sorting.
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters (can include custom sorting)
     * @return Page of messages
     */
    Page<Message> findByConversationId(String conversationId, Pageable pageable);

    /**
     * Finds all messages in a conversation sorted by time (descending).
     * Useful for getting the most recent messages first.
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages (newest first)
     */
    Page<Message> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);

    /**
     * Counts unread messages for a specific recipient.
     * 
     * @param recipientId The recipient user ID
     * @param status Message status (typically SENT)
     * @return Count of unread messages
     */
    long countByRecipientIdAndStatus(String recipientId, MessageStatus status);

    /**
     * Counts unread messages in a specific conversation for a recipient.
     * 
     * @param conversationId The conversation ID
     * @param recipientId The recipient user ID
     * @param status Message status
     * @return Count of unread messages in the conversation
     */
    long countByConversationIdAndRecipientIdAndStatus(
        String conversationId, 
        String recipientId, 
        MessageStatus status
    );

    /**
     * Finds all unread messages for a recipient in a specific conversation.
     * Used when marking messages as read.
     * 
     * @param conversationId The conversation ID
     * @param recipientId The recipient user ID
     * @param status Message status (SENT for unread)
     * @return List of unread messages
     */
    List<Message> findByConversationIdAndRecipientIdAndStatus(
        String conversationId,
        String recipientId,
        MessageStatus status
    );

    /**
     * Finds the most recent message in a conversation.
     * Useful for updating conversation metadata.
     * 
     * @param conversationId The conversation ID
     * @param pageable Should be PageRequest.of(0, 1) to get only 1 result
     * @return Page containing the most recent message
     */
    Page<Message> findTopByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);

    /**
     * Deletes all messages in a conversation.
     * Used when a conversation is deleted.
     * 
     * @param conversationId The conversation ID
     */
    void deleteByConversationId(String conversationId);

    /**
     * Finds messages sent by a specific user with pagination.
     * Useful for user message history.
     * 
     * @param senderId The sender user ID
     * @param pageable Pagination parameters
     * @return Page of messages sent by the user
     */
    Page<Message> findBySenderIdOrderBySentAtDesc(String senderId, Pageable pageable);

    /**
     * Finds messages received by a specific user with pagination.
     * 
     * @param recipientId The recipient user ID
     * @param pageable Pagination parameters
     * @return Page of messages received by the user
     */
    Page<Message> findByRecipientIdOrderBySentAtDesc(String recipientId, Pageable pageable);
}
