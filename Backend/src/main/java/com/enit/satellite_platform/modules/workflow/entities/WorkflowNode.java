package com.enit.satellite_platform.modules.workflow.entities;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowNode {
    private String id;
    private NodeType type;
    private Position position;
    private NodeData data;

    @Data
    public static class NodeData {
        private String label;
        private String description;
        private Map<String, Object> config;
        private NodeStatus status;
    }
}
