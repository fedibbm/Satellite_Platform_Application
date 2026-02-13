package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersion {
    private String version;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private String changelog;
    private String createdBy;
    private LocalDateTime createdAt;
}
