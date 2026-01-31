package com.enit.satellite_platform.modules.messaging.exceptions;

/**
 * Base exception for messaging module operations.
 */
public class MessagingException extends RuntimeException {

    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
