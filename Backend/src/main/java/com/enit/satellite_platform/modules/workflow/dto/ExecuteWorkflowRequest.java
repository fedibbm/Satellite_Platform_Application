package com.enit.satellite_platform.modules.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for executing a workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowRequest {
    
    private String workflowId;
    
    private Map<String, Object> input;
    
    private String correlationId;
    
    private Integer priority; // 0-99, higher number = higher priority
}
