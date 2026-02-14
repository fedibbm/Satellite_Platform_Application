package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow Edge Entity
 * Represents a connection between two nodes in the workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {
    
    private String id;
    
    private String sourceNodeId;
    
    private String targetNodeId;
    
    private String label;
    
    private EdgeCondition condition; // For conditional edges (decision nodes)
}
