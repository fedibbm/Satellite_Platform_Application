package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionContext;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionResult;
import com.enit.satellite_platform.modules.workflow.execution.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TriggerNodeExecutor implements WorkflowNodeExecutor {
    
    @Override
    public String getNodeType() {
        return "trigger";
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.info("Executing trigger node: {}", node.getId());
        
        Map<String, Object> config = node.getData().getConfig();
        String triggerType = (String) config.getOrDefault("triggerType", "manual");
        
        Map<String, Object> result = new HashMap<>();
        result.put("triggered", true);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("triggerType", triggerType);
        result.put("triggeredBy", context.getUserId());
        
        switch (triggerType) {
            case "manual":
                return handleManualTrigger(node, context, result);
            
            case "scheduled":
                return handleScheduledTrigger(node, context, result);
            
            case "webhook":
                return handleWebhookTrigger(node, context, result);
            
            default:
                return NodeExecutionResult.failure("Unknown trigger type: " + triggerType);
        }
    }
    
    private NodeExecutionResult handleManualTrigger(WorkflowNode node, NodeExecutionContext context, Map<String, Object> result) {
        log.info("Manual trigger executed for workflow: {}", context.getWorkflowId());
        
        result.put("executionMode", "manual");
        result.put("parameters", context.getExecutionParameters());
        
        NodeExecutionResult executionResult = NodeExecutionResult.success(result, "Manual trigger executed successfully");
        executionResult.addMetadata("triggerTime", LocalDateTime.now().toString());
        
        return executionResult;
    }
    
    private NodeExecutionResult handleScheduledTrigger(WorkflowNode node, NodeExecutionContext context, Map<String, Object> result) {
        log.info("Scheduled trigger executed for workflow: {}", context.getWorkflowId());
        
        Map<String, Object> config = node.getData().getConfig();
        String schedule = (String) config.get("schedule");
        
        result.put("executionMode", "scheduled");
        result.put("schedule", schedule);
        result.put("nextRun", "Not implemented yet"); // TODO: Calculate next run time
        
        NodeExecutionResult executionResult = NodeExecutionResult.success(result, "Scheduled trigger executed successfully");
        executionResult.addMetadata("schedule", schedule);
        
        return executionResult;
    }
    
    private NodeExecutionResult handleWebhookTrigger(WorkflowNode node, NodeExecutionContext context, Map<String, Object> result) {
        log.info("Webhook trigger executed for workflow: {}", context.getWorkflowId());
        
        Map<String, Object> config = node.getData().getConfig();
        String webhookUrl = (String) config.get("webhookUrl");
        
        result.put("executionMode", "webhook");
        result.put("webhookUrl", webhookUrl);
        result.put("payload", context.getExecutionParameters());
        
        NodeExecutionResult executionResult = NodeExecutionResult.success(result, "Webhook trigger executed successfully");
        executionResult.addMetadata("webhookUrl", webhookUrl);
        
        return executionResult;
    }
    
    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        String triggerType = (String) config.get("triggerType");
        
        if (triggerType == null) {
            return false;
        }
        
        switch (triggerType) {
            case "scheduled":
                return config.containsKey("schedule");
            case "webhook":
                return config.containsKey("webhookUrl");
            case "manual":
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return new NodeMetadata(
                "trigger",
                "Trigger Node",
                "Starts workflow execution via manual, scheduled, or webhook triggers",
                "Control"
        );
    }
}
