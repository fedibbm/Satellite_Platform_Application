package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Workflow Event Entity
 * Stores events that can trigger workflows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_events")
public class WorkflowEvent {
    
    @Id
    private String id;
    
    /**
     * Event type
     * Examples: "IMAGE_UPLOADED", "PROCESSING_COMPLETE", "USER_ACTION", "SCHEDULE_TRIGGERED"
     */
    private String eventType;
    
    /**
     * Event source (where the event came from)
     * Examples: "IMAGE_CONTROLLER", "WEBHOOK", "SCHEDULER", "USER_INTERFACE"
     */
    private String eventSource;
    
    /**
     * Project ID associated with this event
     */
    private String projectId;
    
    /**
     * User ID who triggered the event (if applicable)
     */
    private String userId;
    
    /**
     * Event data/payload
     */
    private Map<String, Object> eventData = new HashMap<>();
    
    /**
     * Event timestamp
     */
    private LocalDateTime timestamp;
    
    /**
     * Whether this event has been processed
     */
    private Boolean processed = false;
    
    /**
     * Workflow executions triggered by this event
     */
    private Map<String, String> triggeredWorkflows = new HashMap<>(); // triggerId -> workflowExecutionId
    
    /**
     * Processing status
     */
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    
    /**
     * Error message if processing failed
     */
    private String errorMessage;
    
    /**
     * Processing timestamp
     */
    private LocalDateTime processedAt;
}
