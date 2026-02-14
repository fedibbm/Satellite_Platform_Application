package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for workflow operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    
    private String id;
    
    private String name;
    
    private String description;
    
    private String projectId;
    
    private String version;
    
    private String status;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<WorkflowNode> nodes;
    
    private List<WorkflowEdge> edges;
    
    private Integer timeoutSeconds;
    
    private String[] tags;
}
