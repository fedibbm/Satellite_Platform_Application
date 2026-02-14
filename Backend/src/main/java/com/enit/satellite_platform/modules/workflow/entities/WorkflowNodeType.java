package com.enit.satellite_platform.modules.workflow.entities;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of workflow node
 */
public enum WorkflowNodeType {
    /**
     * Trigger node - Entry point of workflow execution
     */
    TRIGGER("trigger"),
    
    /**
     * Data input node - Loads or receives data
     */
    DATA_INPUT("data-input"),
    
    /**
     * Processing node - Performs computations or transformations
     */
    PROCESSING("processing"),
    
    /**
     * Decision node - Evaluates conditions and branches execution
     */
    DECISION("decision"),
    
    /**
     * Output node - Saves or returns results
     */
    OUTPUT("output");
    
    private final String value;
    
    WorkflowNodeType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
}
