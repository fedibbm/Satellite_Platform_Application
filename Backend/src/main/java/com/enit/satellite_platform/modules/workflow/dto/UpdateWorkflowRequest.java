package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {
    private String name;
    private String description;
    private String status;
    private List<String> tags;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private String changelog;
}
