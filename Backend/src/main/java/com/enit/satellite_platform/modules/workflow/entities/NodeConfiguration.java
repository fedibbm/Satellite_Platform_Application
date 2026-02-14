package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Node Configuration
 * Stores task-specific configuration that will be mapped to Conductor task properties
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfiguration {
    
    /**
     * Conductor task name to execute
     */
    private String taskName;
    
    /**
     * Task type: SIMPLE, DYNAMIC, FORK_JOIN, DECISION, SWITCH, JOIN, SUB_WORKFLOW, etc.
     */
    private String taskType;
    
    /**
     * Retry configuration
     */
    private RetryConfig retryConfig;
    
    /**
     * Timeout in seconds
     */
    private Integer timeoutSeconds;
    
    /**
     * Whether to wait for completion before proceeding
     */
    private Boolean asyncComplete;
    
    /**
     * Custom configuration properties
     */
    private Map<String, Object> properties = new HashMap<>();
}
