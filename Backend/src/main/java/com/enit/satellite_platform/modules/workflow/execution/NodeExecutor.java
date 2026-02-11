package com.enit.satellite_platform.modules.workflow.execution;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;

import java.util.Map;

/**
 * Interface for all workflow node executors
 */
public interface NodeExecutor {
    
    /**
     * Get the node type this executor handles
     */
    NodeType getNodeType();
    
    /**
     * Execute the node with given context
     */
    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
    
    /**
     * Validate node configuration before execution
     */
    boolean validate(WorkflowNode node);
    
    /**
     * Get metadata about this node type
     */
    NodeMetadata getMetadata();
}
