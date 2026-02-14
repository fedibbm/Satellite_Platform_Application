package com.enit.satellite_platform.modules.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for workflow execution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowResponse {
    
    private String workflowInstanceId;
    
    private String status;
    
    private String message;
}
