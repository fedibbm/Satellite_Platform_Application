package com.enit.satellite_platform.modules.messaging.services;

import com.enit.satellite_platform.modules.messaging.config.MessagingFileStorageConfig;
import com.enit.satellite_platform.modules.messaging.dto.MessageResponse;
import com.enit.satellite_platform.modules.messaging.entities.*;
import com.enit.satellite_platform.modules.messaging.exceptions.*;
import com.enit.satellite_platform.modules.messaging.repositories.ConversationRepository;
import com.enit.satellite_platform.modules.messaging.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing messages and message operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessagingFileStorageConfig fileStorageConfig;

    /**
     * Sends a text message from one user to another.
     */
    @Transactional
    public MessageResponse sendTextMessage(String senderId, String recipientId, String content) {
        log.info("Sending text message from {} to {}", senderId, recipientId);
        
        // Find or create conversation
        Conversation conversation = findOrCreateConversation(senderId, recipientId);
        
        // Create message
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .recipientId(recipientId)
                .messageType(MessageType.TEXT)
                .content(content)
                .sentAt(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();
        
        message = messageRepository.save(message);
        
        // Update conversation metadata
        String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        conversation.updateLastMessage(preview, MessageType.TEXT, senderId);
        conversation.incrementUnreadCount(recipientId);
        conversationRepository.save(conversation);
        
        log.info("Text message sent successfully: {}", message.getId());
        return mapToResponse(message);
    }

    /**
     * Sends an image message from one user to another.
     */
    @Transactional
    public MessageResponse sendImageMessage(String senderId, String recipientId, 
                                           MultipartFile imageFile, String caption) {
        log.info("Sending image message from {} to {}", senderId, recipientId);
        
        // Validate image file
        validateImageFile(imageFile);
        
        // Find or create conversation
        Conversation conversation = findOrCreateConversation(senderId, recipientId);
        
        // Save image file
        String imageUrl = saveImageFile(conversation.getId(), imageFile);
        
        // Create message
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .recipientId(recipientId)
                .messageType(MessageType.IMAGE)
                .content(caption)
                .imageUrl(imageUrl)
                .sentAt(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();
        
        message = messageRepository.save(message);
        
        // Update conversation metadata
        String preview = "📷 Image";
        if (caption != null && !caption.isBlank()) {
            preview = "📷 " + (caption.length() > 97 ? caption.substring(0, 97) + "..." : caption);
        }
        conversation.updateLastMessage(preview, MessageType.IMAGE, senderId);
        conversation.incrementUnreadCount(recipientId);
        conversationRepository.save(conversation);
        
        log.info("Image message sent successfully: {}", message.getId());
        return mapToResponse(message);
    }

    /**
     * Marks a message as read by the recipient.
     */
    @Transactional
    public MessageResponse markAsRead(String messageId, String userId) {
        log.info("Marking message {} as read by user {}", messageId, userId);
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        
        // Verify user is the recipient
        if (!message.getRecipientId().equals(userId)) {
            throw new UnauthorizedAccessException("Only the recipient can mark a message as read");
        }
        
        // Mark as read
        if (message.getStatus() != MessageStatus.READ) {
            message.markAsRead();
            message = messageRepository.save(message);
            
            // Update conversation unread count
            conversationRepository.findById(message.getConversationId())
                    .ifPresent(conversation -> {
                        conversation.resetUnreadCount(userId);
                        conversationRepository.save(conversation);
                    });
        }
        
        return mapToResponse(message);
    }

    /**
     * Marks all messages in a conversation as read for a user.
     */
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) {
        log.info("Marking all messages in conversation {} as read for user {}", conversationId, userId);
        
        // Verify user is participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedAccessException();
        }
        
        // Find all unread messages
        List<Message> unreadMessages = messageRepository.findByConversationIdAndRecipientIdAndStatus(
                conversationId, userId, MessageStatus.SENT);
        
        // Mark each as read
        unreadMessages.forEach(Message::markAsRead);
        messageRepository.saveAll(unreadMessages);
        
        // Reset unread count
        conversation.resetUnreadCount(userId);
        conversationRepository.save(conversation);
        
        log.info("Marked {} messages as read in conversation {}", unreadMessages.size(), conversationId);
    }

    /**
     * Gets paginated message history for a conversation.
     */
    public Page<MessageResponse> getConversationMessages(String conversationId, String userId, 
                                                         int page, int size) {
        log.info("Getting messages for conversation {} (page {}, size {})", conversationId, page, size);
        
        // Verify user is participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedAccessException();
        }
        
        // Get messages sorted by time descending (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);
        
        return messages.map(this::mapToResponse);
    }

    /**
     * Gets total unread message count for a user.
     */
    public long getUnreadMessageCount(String userId) {
        return messageRepository.countByRecipientIdAndStatus(userId, MessageStatus.SENT);
    }

    /**
     * Validates an uploaded image file.
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Image file is required");
        }
        
        // Check file size
        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new InvalidFileException(String.format(
                    "File size exceeds maximum allowed size of %d bytes", 
                    fileStorageConfig.getMaxFileSize()));
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidFileException("Invalid file name");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!fileStorageConfig.isExtensionAllowed(extension)) {
            throw new InvalidFileException(String.format(
                    "File type '%s' is not allowed. Allowed types: %s", 
                    extension, 
                    String.join(", ", fileStorageConfig.getAllowedExtensionsArray())));
        }
    }

    /**
     * Saves an image file to the file system.
     */
    private String saveImageFile(String conversationId, MultipartFile file) {
        try {
            // Get conversation directory
            Path conversationPath = fileStorageConfig.getConversationImagePath(conversationId);
            
            // Create directory if not exists
            if (!Files.exists(conversationPath)) {
                Files.createDirectories(conversationPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = generateUniqueFilename(extension);
            
            // Save file
            Path filePath = conversationPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // Return URL path
            String url = String.format("/api/messaging/images/%s/%s", conversationId, filename);
            log.info("Saved image to: {}", filePath.toAbsolutePath());
            
            return url;
            
        } catch (IOException e) {
            log.error("Failed to save image file", e);
            throw new MessagingException("Failed to save image file", e);
        }
    }

    /**
     * Generates a unique filename for an uploaded image.
     */
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%s.%s", timestamp, uuid, extension);
    }

    /**
     * Extracts file extension from filename.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Finds or creates a conversation between two users.
     */
    private Conversation findOrCreateConversation(String userId1, String userId2) {
        log.debug("Finding or creating conversation between {} and {}", userId1, userId2);
        
        Set<String> participants = new java.util.HashSet<>(java.util.Arrays.asList(userId1, userId2));
        
        // Check if conversation already exists
        java.util.Optional<Conversation> existing = conversationRepository.findByParticipants(participants);
        
        if (existing.isPresent()) {
            log.debug("Found existing conversation: {}", existing.get().getId());
            return existing.get();
        }
        
        // Create new conversation
        Conversation conversation = Conversation.builder()
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .unreadCounts(new java.util.HashMap<>())
                .build();
        
        // Initialize unread counts (using encoded user IDs for MongoDB compatibility)
        conversation.resetUnreadCount(userId1);
        conversation.resetUnreadCount(userId2);
        
        conversation = conversationRepository.save(conversation);
        log.info("Created new conversation: {}", conversation.getId());
        
        return conversation;
    }

    /**
     * Maps Message entity to MessageResponse DTO.
     */
    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .status(message.getStatus())
                .build();
    }
}
