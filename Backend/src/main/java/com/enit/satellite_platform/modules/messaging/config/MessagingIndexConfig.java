package com.enit.satellite_platform.modules.messaging.config;

import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.Index;

import com.enit.satellite_platform.modules.messaging.entities.Message;
import com.enit.satellite_platform.modules.messaging.entities.Conversation;

/**
 * Configuration class for MongoDB indexes for the messaging module.
 * 
 * This ensures all necessary indexes are created on application startup.
 * Indexes defined in @CompoundIndex annotations are auto-created by Spring,
 * but we verify and log them here.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MessagingIndexConfig {

    private final MongoTemplate mongoTemplate;

    /**
     * Ensures all messaging indexes are created on startup.
     * Spring Data MongoDB will auto-create compound indexes from annotations,
     * but this bean logs confirmation and can add any custom indexes.
     */
    @Bean
    public CommandLineRunner initMessagingIndexes() {
        return args -> {
            log.info("Initializing messaging module indexes...");

            // Verify Message indexes
            IndexOperations messageIndexOps = mongoTemplate.indexOps(Message.class);
            log.info("Message collection indexes:");
            messageIndexOps.getIndexInfo().forEach(indexInfo -> 
                log.info("  - {}", indexInfo.getName())
            );

            // Verify Conversation indexes
            IndexOperations conversationIndexOps = mongoTemplate.indexOps(Conversation.class);
            log.info("Conversation collection indexes:");
            conversationIndexOps.getIndexInfo().forEach(indexInfo -> 
                log.info("  - {}", indexInfo.getName())
            );

            log.info("Messaging module indexes initialized successfully.");
        };
    }
}
