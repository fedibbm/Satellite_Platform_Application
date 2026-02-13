package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {
    private String name;
    private String description;
    private String projectId;
    private List<String> tags = new ArrayList<>();
    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<WorkflowEdge> edges = new ArrayList<>();
    private boolean isTemplate = false;
}
