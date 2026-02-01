package com.enit.satellite_platform.modules.messaging.repositories;

import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Repository for Conversation entity.
 * 
 * Key features:
 * - Find conversations by participant
 * - Find or create conversation between two users
 * - Paginated conversation list
 */
@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /**
     * Finds all conversations for a specific user, sorted by most recent activity.
     * 
     * Usage: For displaying a user's conversation list
     * 
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of conversations sorted by lastMessageAt descending
     */
    @Query("{ 'participants': ?0 }")
    Page<Conversation> findByParticipantsContaining(String userId, Pageable pageable);

    /**
     * Finds all conversations for a specific user (non-paginated).
     * 
     * @param userId The user ID
     * @return List of all conversations
     */
    @Query("{ 'participants': ?0 }")
    java.util.List<Conversation> findByParticipantsContaining(String userId);

    /**
     * Finds a conversation between two specific users.
     * Uses $all to match both participants regardless of order, and $size to ensure exactly 2.
     * 
     * Usage: To check if a conversation already exists before creating a new one
     * 
     * @param participants Set containing exactly 2 user IDs
     * @return Optional containing the conversation if found
     */
    @Query("{ 'participants': { $all: ?0, $size: 2 } }")
    Optional<Conversation> findByParticipants(Set<String> participants);

    /**
     * Checks if a conversation exists between two users.
     * 
     * @param participants Set of 2 user IDs
     * @return true if conversation exists
     */
    @Query("{ 'participants': { $all: ?0, $size: 2 } }")
    boolean existsByParticipants(Set<String> participants);

    /**
     * Counts total conversations for a user.
     * 
     * @param userId The user ID
     * @return Count of conversations
     */
    @Query(value = "{ 'participants': ?0 }", count = true)
    long countByParticipantsContaining(String userId);

    /**
     * Finds conversations where a user is a participant,
     * sorted by most recent activity, with pagination.
     * 
     * This is the main query for the conversation list UI.
     * 
     * @param userId The user ID
     * @param pageable Pagination with sort by lastMessageAt DESC
     * @return Page of conversations
     */
    @Query(value = "{ 'participants': ?0 }", sort = "{ 'lastMessageAt': -1 }")
    Page<Conversation> findUserConversationsSortedByRecent(String userId, Pageable pageable);
}
