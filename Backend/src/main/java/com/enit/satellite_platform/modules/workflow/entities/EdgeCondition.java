package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Edge Condition
 * Defines conditional logic for decision nodes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeCondition {
    
    private String expression; // JavaScript expression for evaluation
    
    private String operator; // EQUALS, NOT_EQUALS, GREATER_THAN, etc.
    
    private Object value;
    
    private String parameter; // Which output parameter to evaluate
}
