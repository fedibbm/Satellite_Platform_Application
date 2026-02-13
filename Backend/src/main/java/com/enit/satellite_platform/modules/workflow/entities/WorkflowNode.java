package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {
    private String id;
    private String type; // trigger, data-input, processing, decision, output
    private Position position;
    private NodeData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeData {
        private String label;
        private String description;
        private Map<String, Object> config;
    }
}
