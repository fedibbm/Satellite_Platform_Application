package com.enit.satellite_platform.modules.messaging.entities;

/**
 * Enum representing the delivery and read status of a message.
 */
public enum MessageStatus {
    SENT,    // Message has been sent but not yet read
    READ     // Message has been read by the recipient
}
