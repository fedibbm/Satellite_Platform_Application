package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowVersion {
    private String version;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private String changelog;
}
