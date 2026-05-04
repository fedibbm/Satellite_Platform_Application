package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowVersionDTO {
    private String version;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private String changelog;
    private boolean isCurrent;
}
