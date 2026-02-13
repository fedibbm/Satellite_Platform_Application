package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {
    private String id;
    private String source;
    private String target;
    private String sourceHandle;
    private String targetHandle;
    private String label;
}
