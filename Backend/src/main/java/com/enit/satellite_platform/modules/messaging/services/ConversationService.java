package com.enit.satellite_platform.modules.messaging.services;

import com.enit.satellite_platform.modules.messaging.dto.ConversationResponse;
import com.enit.satellite_platform.modules.messaging.entities.Conversation;
import com.enit.satellite_platform.modules.messaging.exceptions.ConversationNotFoundException;
import com.enit.satellite_platform.modules.messaging.exceptions.UnauthorizedAccessException;
import com.enit.satellite_platform.modules.messaging.repositories.ConversationRepository;
import com.enit.satellite_platform.modules.messaging.websocket.UserPresenceService;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
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
    private final UserRepository userRepository;
    private final UserPresenceService userPresenceService;

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
        
        // Initialize unread counts (using encoded user IDs for MongoDB compatibility)
        conversation.resetUnreadCount(userId1);
        conversation.resetUnreadCount(userId2);
        
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
        
        // Count those with unread messages (using the getUnreadCount method which handles encoding)
        return conversations.stream()
                .filter(conv -> conv.getUnreadCount(userId) > 0)
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
        Integer unreadCount = conversation.getUnreadCount(currentUserId);
        
        // Fetch other participant's info from User repository
        String otherParticipantName = null;
        try {
            log.debug("Looking up user with ID: {}", otherParticipantId);
            
            // Convert to ObjectId and find user
            ObjectId userId = new ObjectId(otherParticipantId);
            Optional<User> otherUser = userRepository.findById(userId);
            
            if (otherUser.isPresent()) {
                User user = otherUser.get();
                String name = user.getName();
                
                // Use the name field if available and not empty
                if (name != null && !name.trim().isEmpty()) {
                    otherParticipantName = name;
                    log.debug("Found user {} with name: {}", otherParticipantId, otherParticipantName);
                } else {
                    // If name is empty, extract username from email
                    String email = user.getEmail();
                    otherParticipantName = email != null ? email.split("@")[0] : "Unknown";
                    log.debug("User {} has no name field, using email prefix: {}", otherParticipantId, otherParticipantName);
                }
            } else {
                log.warn("User not found with ID: {}", otherParticipantId);
                otherParticipantName = "Unknown User";
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid ObjectId format for {}: {}", otherParticipantId, e.getMessage());
            otherParticipantName = "Unknown User";
        } catch (Exception e) {
            log.error("Failed to fetch user info for {}: {}", otherParticipantId, e.getMessage(), e);
            otherParticipantName = "Unknown User";
        }
        
        // Check if other participant is online
        boolean isOnline = userPresenceService.isUserOnline(otherParticipantId);
        
        ConversationResponse response = ConversationResponse.builder()
                .id(conversation.getId())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageType(conversation.getLastMessageType())
                .lastMessageSenderId(conversation.getLastMessageSenderId())
                .unreadCount(unreadCount)
                .otherParticipantId(otherParticipantId)
                .otherParticipantName(otherParticipantName) // Set the actual username
                .otherParticipantOnline(isOnline) // Set current online status
                .build();
        
        log.debug("Mapped conversation response with otherParticipantName: {}, online: {}", otherParticipantName, isOnline);
        return response;
    }
}
