package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowStatus;
import lombok.Data;
import java.util.List;

@Data
public class UpdateWorkflowRequest {
    private String name;
    private String description;
    private WorkflowStatus status;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private String changelog;
}
