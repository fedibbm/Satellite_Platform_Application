package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.NodeType;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for trigger nodes
 * Triggers can be manual, scheduled, or event-based
 */
@Component
public class TriggerNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TriggerNodeExecutor.class);

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        logger.info("Executing trigger node: {}", node.getId());

        try {
            Map<String, Object> config = node.getData().getConfig();
            String triggerType = config != null ? 
                (String) config.getOrDefault("triggerType", "manual") : "manual";

            Map<String, Object> result = new HashMap<>();
            result.put("triggered", true);
            result.put("triggerType", triggerType);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("triggeredBy", context.getUserId());

            logger.info("Trigger node executed successfully: {}", node.getId());
            return NodeExecutionResult.success(result);

        } catch (Exception e) {
            logger.error("Error executing trigger node: {}", node.getId(), e);
            return NodeExecutionResult.failure("Trigger execution failed: " + e.getMessage());
        }
    }

    @Override
    public boolean validate(WorkflowNode node) {
        // Trigger nodes don't have strict validation requirements
        return true;
    }

    @Override
    public NodeMetadata getMetadata() {
        Map<String, String> schema = new HashMap<>();
        schema.put("triggerType", "String: manual, scheduled, event");
        schema.put("cron", "String: Cron expression for scheduled triggers");

        return new NodeMetadata(
            "Trigger",
            "Initiates workflow execution",
            "Control",
            schema,
            new ArrayList<>(),
            java.util.List.of("triggered", "timestamp")
        );
    }
}
