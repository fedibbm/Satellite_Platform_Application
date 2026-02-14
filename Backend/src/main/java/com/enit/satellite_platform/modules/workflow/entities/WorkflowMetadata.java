package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow Metadata
 * Additional workflow configuration and metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMetadata {
    
    /**
     * Workflow timeout in seconds
     */
    private Integer timeoutSeconds;
    
    /**
     * Workflow schema version
     */
    private String schemaVersion;
    
    /**
     * Tags for categorization
     */
    private String[] tags;
    
    /**
     * Whether the workflow can be paused/resumed
     */
    private Boolean restartable;
    
    /**
     * Custom metadata properties
     */
    private Map<String, Object> customProperties = new HashMap<>();
}
