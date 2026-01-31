package com.enit.satellite_platform.modules.messaging.exceptions;

/**
 * Exception thrown when a message is not found.
 */
public class MessageNotFoundException extends MessagingException {

    public MessageNotFoundException(String messageId) {
        super("Message not found: " + messageId);
    }
}
