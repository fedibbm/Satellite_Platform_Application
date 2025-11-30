package com.enit.satellite_platform.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for the application using Redis.
 * 
 * This configuration enables Spring Cache abstraction with Redis as the cache provider.
 * Different cache regions are configured with specific TTL (Time To Live) values
 * to optimize memory usage and data freshness.
 * 
 * Cache Regions:
 * - projects: User projects (TTL: 10 minutes) - frequently accessed
 * - images: Image metadata (TTL: 30 minutes) - relatively stable
 * - users: User information (TTL: 1 hour) - rarely changes
 * - processingResults: Analysis results (TTL: 24 hours) - expensive to recompute
 * - dashboard: Dashboard statistics (TTL: 5 minutes) - needs frequent refresh
 * - projectList: Project listings (TTL: 5 minutes) - changes with user actions
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the Redis Cache Manager with custom TTL for different cache regions.
     * 
     * @param connectionFactory Redis connection factory
     * @return Configured CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // Don't cache null values

        // Custom TTL configurations per cache region
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Projects cache - TTL: 10 minutes (frequently modified)
        cacheConfigurations.put("projects", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Images metadata cache - TTL: 30 minutes (stable once uploaded)
        cacheConfigurations.put("images", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Users cache - TTL: 1 hour (rarely changes)
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Processing results cache - TTL: 24 hours (expensive to recompute)
        cacheConfigurations.put("processingResults", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // Dashboard statistics cache - TTL: 5 minutes (needs frequent updates)
        cacheConfigurations.put("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Project list cache - TTL: 5 minutes (changes with CRUD operations)
        cacheConfigurations.put("projectList", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Image list cache - TTL: 10 minutes
        cacheConfigurations.put("imageList", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Short-lived cache for API responses - TTL: 2 minutes
        cacheConfigurations.put("apiResponse", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
