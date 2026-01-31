package com.enit.satellite_platform.modules.messaging.exceptions;

/**
 * Exception thrown when an uploaded file is invalid.
 */
public class InvalidFileException extends MessagingException {

    public InvalidFileException(String message) {
        super(message);
    }
}
