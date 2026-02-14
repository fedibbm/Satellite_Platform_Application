package com.enit.satellite_platform.modules.workflow.entities;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the execution status of a workflow or node
 */
public enum WorkflowExecutionStatus {
    /**
     * Execution is pending/queued but not started yet
     */
    PENDING("pending"),
    
    /**
     * Currently running/executing
     */
    RUNNING("running"),
    
    /**
     * Successfully completed
     */
    COMPLETED("completed"),
    
    /**
     * Execution failed with errors
     */
    FAILED("failed"),
    
    /**
     * Execution was cancelled by user
     */
    CANCELLED("cancelled");
    
    private final String value;
    
    WorkflowExecutionStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
}
