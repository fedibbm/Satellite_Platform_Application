package com.enit.satellite_platform.modules.messaging.exceptions;

/**
 * Exception thrown when a conversation is not found.
 */
public class ConversationNotFoundException extends MessagingException {

    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
