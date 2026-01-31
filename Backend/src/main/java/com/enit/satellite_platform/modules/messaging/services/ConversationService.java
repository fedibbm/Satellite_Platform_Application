package com.enit.satellite_platform.modules.messaging.services;

import com.enit.satellite_platform.modules.messaging.dto.ConversationResponse;
import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.exceptions.ConversationNotFoundException;
import com.enit.satellite_platform.modules.messaging.exceptions.UnauthorizedAccessException;
import com.enit.satellite_platform.modules.messaging.repositories.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing conversations and conversation-related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * Finds or creates a conversation between two users.
     */
    @Transactional
    public Conversation findOrCreateConversation(String userId1, String userId2) {
        log.debug("Finding or creating conversation between {} and {}", userId1, userId2);
        
        Set<String> participants = new HashSet<>(Arrays.asList(userId1, userId2));
        
        // Check if conversation already exists
        Optional<Conversation> existing = conversationRepository.findByParticipants(participants);
        
        if (existing.isPresent()) {
            log.debug("Found existing conversation: {}", existing.get().getId());
            return existing.get();
        }
        
        // Create new conversation
        Conversation conversation = Conversation.builder()
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .unreadCounts(new HashMap<>())
                .build();
        
        // Initialize unread counts
        conversation.getUnreadCounts().put(userId1, 0);
        conversation.getUnreadCounts().put(userId2, 0);
        
        conversation = conversationRepository.save(conversation);
        log.info("Created new conversation: {}", conversation.getId());
        
        return conversation;
    }

    /**
     * Gets all conversations for a user (paginated and sorted by recent activity).
     */
    public Page<ConversationResponse> getUserConversations(String userId, int page, int size) {
        log.info("Getting conversations for user {} (page {}, size {})", userId, page, size);
        
        // Sort by most recent message first
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        
        Page<Conversation> conversations = conversationRepository.findByParticipantsContaining(userId, pageable);
        
        return conversations.map(conv -> mapToResponse(conv, userId));
    }

    /**
     * Gets a specific conversation by ID.
     */
    public ConversationResponse getConversation(String conversationId, String userId) {
        log.info("Getting conversation {} for user {}", conversationId, userId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        // Verify user is a participant
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedAccessException();
        }
        
        return mapToResponse(conversation, userId);
    }

    /**
     * Checks if a conversation exists between two users.
     */
    public boolean conversationExists(String userId1, String userId2) {
        Set<String> participants = new HashSet<>(Arrays.asList(userId1, userId2));
        return conversationRepository.existsByParticipants(participants);
    }

    /**
     * Gets the count of conversations with unread messages for a user.
     */
    public long getConversationsWithUnreadCount(String userId) {
        // Find all user's conversations
        List<Conversation> conversations = conversationRepository.findByParticipantsContaining(userId);
        
        // Count those with unread messages
        return conversations.stream()
                .filter(conv -> {
                    Integer unreadCount = conv.getUnreadCounts().get(userId);
                    return unreadCount != null && unreadCount > 0;
                })
                .count();
    }

    /**
     * Deletes a conversation (admin or participant action).
     */
    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        log.info("Deleting conversation {} by user {}", conversationId, userId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        // Verify user is a participant
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedAccessException();
        }
        
        conversationRepository.delete(conversation);
        
        // TODO: Consider also deleting all messages in this conversation
        // or implementing soft delete for privacy/legal reasons
        
        log.info("Deleted conversation: {}", conversationId);
    }

    /**
     * Maps Conversation entity to ConversationResponse DTO with enriched user info.
     */
    private ConversationResponse mapToResponse(Conversation conversation, String currentUserId) {
        // Get the other participant's ID
        String otherParticipantId = conversation.getOtherParticipant(currentUserId);
        
        // Get unread count for current user
        Integer unreadCount = conversation.getUnreadCounts().getOrDefault(currentUserId, 0);
        
        ConversationResponse response = ConversationResponse.builder()
                .id(conversation.getId())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageType(conversation.getLastMessageType())
                .lastMessageSenderId(conversation.getLastMessageSenderId())
                .unreadCount(unreadCount)
                .otherParticipantId(otherParticipantId)
                .build();
        
        // TODO: Enrich with user information from User service
        // For now, we'll leave these fields null
        // In a real implementation, you would call the User service to get:
        // - otherParticipantName
        // - otherParticipantAvatar
        // - otherParticipantOnline status
        
        return response;
    }
}
