package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {
    
    private String name;
    
    private String description;
    
    private String projectId;
    
    private List<WorkflowNode> nodes;
    
    private List<WorkflowEdge> edges;
    
    private Integer timeoutSeconds;
    
    private String[] tags;
}
