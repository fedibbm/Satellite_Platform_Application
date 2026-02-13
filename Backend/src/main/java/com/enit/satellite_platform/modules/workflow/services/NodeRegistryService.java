package com.enit.satellite_platform.modules.workflow.services;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NodeRegistryService {
    
    private final Map<String, NodeMetadata> nodeRegistry = new HashMap<>();
    
    public NodeRegistryService() {
        // Register built-in node types
        registerNodeType("trigger", createTriggerMetadata());
        registerNodeType("data-input", createDataInputMetadata());
        registerNodeType("processing", createProcessingMetadata());
        registerNodeType("decision", createDecisionMetadata());
        registerNodeType("output", createOutputMetadata());
    }
    
    public void registerNodeType(String type, NodeMetadata metadata) {
        nodeRegistry.put(type, metadata);
    }
    
    public NodeMetadata getNodeMetadata(String type) {
        return nodeRegistry.get(type);
    }
    
    public List<NodeMetadata> getAllNodeTypes() {
        return new ArrayList<>(nodeRegistry.values());
    }
    
    public boolean isValidNodeType(String type) {
        return nodeRegistry.containsKey(type);
    }
    
    private NodeMetadata createTriggerMetadata() {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setType("trigger");
        metadata.setName("Trigger");
        metadata.setDescription("Starts the workflow execution");
        metadata.setCategory("Control");
        metadata.setIcon("play");
        metadata.setConfigSchema(Map.of(
            "triggerType", Map.of("type", "string", "enum", List.of("manual", "scheduled", "webhook")),
            "schedule", Map.of("type", "string", "description", "Cron expression for scheduled trigger")
        ));
        return metadata;
    }
    
    private NodeMetadata createDataInputMetadata() {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setType("data-input");
        metadata.setName("Data Input");
        metadata.setDescription("Fetches data from external sources");
        metadata.setCategory("Input");
        metadata.setIcon("download");
        metadata.setConfigSchema(Map.of(
            "sourceType", Map.of("type", "string", "enum", List.of("gee", "project", "storage")),
            "collection_id", Map.of("type", "string"),
            "start_date", Map.of("type", "string", "format", "date"),
            "end_date", Map.of("type", "string", "format", "date")
        ));
        return metadata;
    }
    
    private NodeMetadata createProcessingMetadata() {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setType("processing");
        metadata.setName("Processing");
        metadata.setDescription("Processes data using various algorithms");
        metadata.setCategory("Processing");
        metadata.setIcon("cpu");
        metadata.setConfigSchema(Map.of(
            "processingType", Map.of("type", "string", "enum", List.of("ndvi", "evi", "custom")),
            "parameters", Map.of("type", "object")
        ));
        return metadata;
    }
    
    private NodeMetadata createDecisionMetadata() {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setType("decision");
        metadata.setName("Decision");
        metadata.setDescription("Routes execution based on conditions");
        metadata.setCategory("Control");
        metadata.setIcon("branch");
        metadata.setConfigSchema(Map.of(
            "condition", Map.of("type", "string"),
            "operator", Map.of("type", "string", "enum", List.of("equals", "greater", "less", "contains"))
        ));
        return metadata;
    }
    
    private NodeMetadata createOutputMetadata() {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setType("output");
        metadata.setName("Output");
        metadata.setDescription("Saves or exports workflow results");
        metadata.setCategory("Output");
        metadata.setIcon("save");
        metadata.setConfigSchema(Map.of(
            "outputType", Map.of("type", "string", "enum", List.of("storage", "project", "notification")),
            "destination", Map.of("type", "string")
        ));
        return metadata;
    }
    
    public static class NodeMetadata {
        private String type;
        private String name;
        private String description;
        private String category;
        private String icon;
        private Map<String, Object> configSchema;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public Map<String, Object> getConfigSchema() { return configSchema; }
        public void setConfigSchema(Map<String, Object> configSchema) { this.configSchema = configSchema; }
    }
}
