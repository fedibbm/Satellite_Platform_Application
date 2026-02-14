package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow Node Entity
 * Represents a single task/step in the workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {
    
    private String id;
    
    private String type; // TRIGGER, TASK, DECISION, FORK_JOIN, SUB_WORKFLOW, etc.
    
    private String name;
    
    private String description;
    
    private NodePosition position; // UI position (x, y coordinates)
    
    private NodeConfiguration configuration;
    
    /**
     * Input parameters for this node
     */
    private Map<String, Object> inputParameters = new HashMap<>();
    
    /**
     * Task reference name - unique identifier for this task instance in the workflow
     */
    private String taskReferenceName;
}
