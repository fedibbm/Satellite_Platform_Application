package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateWorkflowRequest {
    @NotBlank(message = "Workflow name is required")
    private String name;
    
    private String description;
    private String projectId;
    private Boolean isTemplate;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
}
