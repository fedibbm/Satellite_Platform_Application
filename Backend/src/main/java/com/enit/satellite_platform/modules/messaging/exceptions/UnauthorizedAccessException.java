package com.enit.satellite_platform.modules.messaging.exceptions;

/**
 * Exception thrown when a user tries to access a conversation they're not part of.
 */
public class UnauthorizedAccessException extends MessagingException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException() {
        super("You are not authorized to access this conversation");
    }
}
