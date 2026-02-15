package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {
    
    private String name;
    
    private String description;
    
    private String status; // DRAFT, PUBLISHED, ARCHIVED
    
    private List<WorkflowNode> nodes;
    
    private List<WorkflowEdge> edges;
}
