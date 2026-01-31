package com.enit.satellite_platform.modules.messaging.controllers;

import com.enit.satellite_platform.modules.messaging.config.MessagingFileStorageConfig;
import com.enit.satellite_platform.modules.messaging.dto.*;
import com.enit.satellite_platform.modules.messaging.exceptions.InvalidFileException;
import com.enit.satellite_platform.modules.messaging.exceptions.UnauthorizedAccessException;
import com.enit.satellite_platform.modules.messaging.services.ConversationService;
import com.enit.satellite_platform.modules.messaging.services.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * REST Controller for messaging operations.
 * 
 * Endpoints:
 * - POST /api/messaging/messages - Send text message
 * - POST /api/messaging/messages/image - Send image message
 * - GET /api/messaging/conversations - List user's conversations
 * - GET /api/messaging/conversations/{id}/messages - Get conversation messages
 * - PUT /api/messaging/messages/{id}/read - Mark message as read
 * - PUT /api/messaging/conversations/{id}/read - Mark all messages as read
 * - GET /api/messaging/unread-count - Get unread message count
 * - GET /api/messaging/images/{conversationId}/{filename} - Serve image files
 */
@Slf4j
@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class MessagingController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final MessagingFileStorageConfig fileStorageConfig;

    /**
     * Sends a text message.
     * 
     * POST /api/messaging/messages
     * Body: { "recipientId": "user123", "content": "Hello!" }
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendTextMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        
        String senderId = authentication.getName(); // Assumes user ID is in authentication
        
        MessageResponse response = messageService.sendTextMessage(
                senderId, 
                request.getRecipientId(), 
                request.getContent());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Sends an image message with optional caption.
     * 
     * POST /api/messaging/messages/image
     * Content-Type: multipart/form-data
     * Form fields:
     * - recipientId: string
     * - image: file
     * - caption: string (optional)
     */
    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> sendImageMessage(
            @RequestParam("recipientId") String recipientId,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "caption", required = false) String caption,
            Authentication authentication) {
        
        String senderId = authentication.getName();
        
        MessageResponse response = messageService.sendImageMessage(
                senderId, 
                recipientId, 
                image, 
                caption);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all conversations for the authenticated user.
     * 
     * GET /api/messaging/conversations?page=0&size=20
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        Page<ConversationResponse> conversations = conversationService.getUserConversations(
                userId, page, size);
        
        return ResponseEntity.ok(conversations);
    }

    /**
     * Gets a specific conversation by ID.
     * 
     * GET /api/messaging/conversations/{id}
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        ConversationResponse conversation = conversationService.getConversation(id, userId);
        
        return ResponseEntity.ok(conversation);
    }

    /**
     * Gets messages in a conversation with pagination.
     * 
     * GET /api/messaging/conversations/{id}/messages?page=0&size=50
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Page<MessageResponse>> getConversationMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        Page<MessageResponse> messages = messageService.getConversationMessages(
                id, userId, page, size);
        
        return ResponseEntity.ok(messages);
    }

    /**
     * Marks a specific message as read.
     * 
     * PUT /api/messaging/messages/{id}/read
     */
    @PutMapping("/messages/{id}/read")
    public ResponseEntity<MessageResponse> markMessageAsRead(
            @PathVariable String id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        MessageResponse response = messageService.markAsRead(id, userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Marks all messages in a conversation as read.
     * 
     * PUT /api/messaging/conversations/{id}/read
     */
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable String id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        messageService.markConversationAsRead(id, userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Gets total unread message count for the authenticated user.
     * 
     * GET /api/messaging/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        
        String userId = authentication.getName();
        
        long totalUnread = messageService.getUnreadMessageCount(userId);
        long conversationsWithUnread = conversationService.getConversationsWithUnreadCount(userId);
        
        UnreadCountResponse response = UnreadCountResponse.builder()
                .totalUnreadCount(totalUnread)
                .conversationsWithUnread(conversationsWithUnread)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Serves an image file from a conversation.
     * 
     * GET /api/messaging/images/{conversationId}/{filename}
     */
    @GetMapping("/images/{conversationId}/{filename}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String conversationId,
            @PathVariable String filename,
            Authentication authentication) throws IOException {
        
        String userId = authentication.getName();
        
        // Verify user is participant in the conversation
        conversationService.getConversation(conversationId, userId);
        
        // Sanitize filename to prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new InvalidFileException("Invalid filename");
        }
        
        // Get file path
        Path imagePath = fileStorageConfig.getConversationImagePath(conversationId).resolve(filename);
        
        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        
        // Load file as resource
        Resource resource = new UrlResource(imagePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        
        // Determine content type
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400") // Cache for 1 day
                .body(resource);
    }

    /**
     * Deletes a conversation.
     * 
     * DELETE /api/messaging/conversations/{id}
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        conversationService.deleteConversation(id, userId);
        
        return ResponseEntity.noContent().build();
    }
}
