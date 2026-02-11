package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;

@Data
public class WorkflowEdge {
    private String id;
    private String source;
    private String target;
    private String label;
    private String type; // "default" or "conditional"
}
