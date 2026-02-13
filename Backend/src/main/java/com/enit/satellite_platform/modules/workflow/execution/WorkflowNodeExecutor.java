package com.enit.satellite_platform.modules.workflow.execution;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;

public interface WorkflowNodeExecutor {
    
    /**
     * Get the node type this executor handles
     */
    String getNodeType();
    
    /**
     * Execute the node with the given configuration and context
     */
    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
    
    /**
     * Validate the node configuration before execution
     */
    boolean validate(WorkflowNode node);
    
    /**
     * Get metadata about this node type
     */
    NodeMetadata getMetadata();
    
    class NodeMetadata {
        private final String type;
        private final String name;
        private final String description;
        private final String category;
        
        public NodeMetadata(String type, String name, String description, String category) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.category = category;
        }
        
        public String getType() { return type; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }
}
