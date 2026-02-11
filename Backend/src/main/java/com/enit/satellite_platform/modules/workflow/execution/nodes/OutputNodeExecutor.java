package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for output nodes
 * Handles saving results to various destinations
 */
@Component
public class OutputNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(OutputNodeExecutor.class);

    @Override
    public NodeType getNodeType() {
        return NodeType.OUTPUT;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing output node: {}", node.getId());

        try {
            Map<String, Object> config = node.getData().getConfig();
            String outputType = config != null ? 
                (String) config.getOrDefault("outputType", "project") : "project";

            Map<String, Object> result = new HashMap<>();
            result.put("saved", true);
            result.put("outputType", outputType);
            result.put("location", "/output/" + node.getId());

            // In a real implementation, this would:
            // 1. Get data from previous nodes via context.getNodeOutputs()
            // 2. Save to the specified destination (project, storage, etc.)
            // 3. Return the saved location/reference

            logger.info("Output node executed successfully: {}", node.getId());
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing output node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Output execution failed: " + e.getMessage());
        }
    }

    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        if (config == null) {
            return false;
        }
        // Add validation logic here
        return true;
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("outputType", "String: project, storage, export");
        schema.put("format", "String: geotiff, png, json");

        return new NodeMetadata(
            "Output",
            "Saves workflow results to specified destination",
            "Output",
            schema,
            java.util.List.of("data"),
            java.util.List.of("saved", "location")
        );
    }
}
