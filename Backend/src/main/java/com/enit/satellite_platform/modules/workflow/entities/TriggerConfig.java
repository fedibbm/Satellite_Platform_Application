package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Trigger Configuration
 * Contains type-specific configuration for different trigger types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriggerConfig {
    
    // ========== SCHEDULED TRIGGER CONFIG ==========
    
    /**
     * Cron expression for scheduled triggers
     * Examples:
     * - "0 0 * * * *" - Every hour
     * - "0 0 0 * * *" - Every day at midnight
     * - "0 0 12 * * MON-FRI" - Every weekday at noon
     */
    private String cronExpression;
    
    /**
     * Timezone for cron expression
     * Default: UTC
     */
    private String timezone = ZoneId.of("UTC").toString();
    
    /**
     * Maximum number of executions (null = unlimited)
     */
    private Integer maxExecutions;
    
    /**
     * Start date for scheduled triggers
     */
    private String startDate;
    
    /**
     * End date for scheduled triggers
     */
    private String endDate;
    
    // ========== WEBHOOK TRIGGER CONFIG ==========
    
    /**
     * Webhook secret key for validation
     * Used to verify webhook authenticity
     */
    private String webhookSecret;
    
    /**
     * Allowed HTTP methods for webhook (GET, POST, PUT)
     */
    private String[] allowedMethods = new String[]{"POST"};
    
    /**
     * IP whitelist for webhook access (optional)
     */
    private String[] ipWhitelist;
    
    /**
     * Custom headers required for webhook
     */
    private Map<String, String> requiredHeaders = new HashMap<>();
    
    /**
     * Path parameter mapping (webhook path params to workflow inputs)
     */
    private Map<String, String> pathParamMapping = new HashMap<>();
    
    /**
     * Query parameter mapping (webhook query params to workflow inputs)
     */
    private Map<String, String> queryParamMapping = new HashMap<>();
    
    /**
     * Request body mapping (webhook body fields to workflow inputs)
     */
    private Map<String, String> bodyMapping = new HashMap<>();
    
    // ========== EVENT TRIGGER CONFIG ==========
    
    /**
     * Event type to listen for
     * Examples: "IMAGE_UPLOADED", "PROCESSING_COMPLETE", "USER_ACTION"
     */
    private String eventType;
    
    /**
     * Event source filter (optional)
     * Only trigger on events from specific source
     */
    private String eventSource;
    
    /**
     * Event data filter conditions (optional)
     * JSON path expressions to filter events
     */
    private Map<String, Object> eventFilters = new HashMap<>();
    
    /**
     * Event data mapping (event data to workflow inputs)
     */
    private Map<String, String> eventDataMapping = new HashMap<>();
    
    // ========== COMMON CONFIG ==========
    
    /**
     * Retry configuration if trigger fails
     */
    private Integer retryAttempts = 0;
    
    /**
     * Retry delay in seconds
     */
    private Integer retryDelaySeconds = 60;
    
    /**
     * Timeout for trigger execution in seconds
     */
    private Integer timeoutSeconds = 300;
    
    /**
     * Additional custom configuration
     */
    private Map<String, Object> customConfig = new HashMap<>();
}
