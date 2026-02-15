package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Workflow Trigger Entity
 * Defines how and when a workflow should be triggered
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_triggers")
public class WorkflowTrigger {
    
    @Id
    private String id;
    
    /**
     * Trigger name
     */
    private String name;
    
    /**
     * Description of the trigger
     */
    private String description;
    
    /**
     * Workflow definition ID to trigger
     */
    private String workflowDefinitionId;
    
    /**
     * Project ID
     */
    private String projectId;
    
    /**
     * Trigger type: SCHEDULED, WEBHOOK, EVENT, MANUAL
     */
    private TriggerType type;
    
    /**
     * Trigger configuration (cron expression, webhook secret, event type, etc.)
     */
    private TriggerConfig config;
    
    /**
     * Default input parameters to pass to the workflow
     */
    private Map<String, Object> defaultInputs = new HashMap<>();
    
    /**
     * Whether this trigger is active
     */
    private Boolean enabled = true;
    
    /**
     * Created by user ID
     */
    private String createdBy;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;
    
    /**
     * Last execution timestamp
     */
    private LocalDateTime lastExecutedAt;
    
    /**
     * Total execution count
     */
    private Long executionCount = 0L;
    
    /**
     * Last execution status
     */
    private String lastExecutionStatus;
    
    /**
     * Last execution workflow ID
     */
    private String lastExecutionWorkflowId;
    
    public enum TriggerType {
        SCHEDULED,   // Cron-based scheduled execution
        WEBHOOK,     // External HTTP webhook
        EVENT,       // Internal application event
        MANUAL       // Manual trigger only (no automatic execution)
    }
}
