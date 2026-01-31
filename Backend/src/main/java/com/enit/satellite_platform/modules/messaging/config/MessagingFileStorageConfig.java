package com.enit.satellite_platform.modules.messaging.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for messaging file storage.
 * Handles image uploads for the messaging system.
 */
@Getter
@Configuration
public class MessagingFileStorageConfig {

    @Value("${messaging.upload.directory:upload-dir/messages}")
    private String uploadDirectory;

    @Value("${messaging.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${messaging.upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    /**
     * Creates the upload directory on application startup if it doesn't exist.
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created messaging upload directory: " + uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create messaging upload directory: " + e.getMessage());
        }
    }

    /**
     * Gets the allowed file extensions as an array.
     */
    public String[] getAllowedExtensionsArray() {
        return allowedExtensions.split(",");
    }

    /**
     * Validates if a file extension is allowed.
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null) {
            return false;
        }
        String normalized = extension.toLowerCase().replace(".", "");
        for (String allowed : getAllowedExtensionsArray()) {
            if (allowed.trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the full path for a conversation's image directory.
     */
    public Path getConversationImagePath(String conversationId) {
        return Paths.get(uploadDirectory, conversationId);
    }
}
