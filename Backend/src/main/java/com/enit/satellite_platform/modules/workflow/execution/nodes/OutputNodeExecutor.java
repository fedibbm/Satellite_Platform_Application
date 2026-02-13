package com.enit.satellite_platform.modules.workflow.execution.nodes;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionContext;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionResult;
import com.enit.satellite_platform.modules.workflow.execution.WorkflowNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutputNodeExecutor implements WorkflowNodeExecutor {
    
    @Override
    public String getNodeType() {
        return "output";
    }
    
    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.info("Executing output node: {}", node.getId());
        
        Map<String, Object> config = node.getData().getConfig();
        String outputType = (String) config.getOrDefault("outputType", "storage");
        
        // Get data from previous nodes
        Object outputData = getPreviousNodeOutput(context, node);
        
        try {
            switch (outputType) {
                case "storage":
                    return saveToStorage(node, context, config, outputData);
                
                case "project":
                    return saveToProject(node, context, config, outputData);
                
                case "notification":
                    return sendNotification(node, context, config, outputData);
                
                case "report":
                    return generateReport(node, context, config, outputData);
                
                default:
                    return NodeExecutionResult.failure("Unknown output type: " + outputType);
            }
        } catch (Exception e) {
            log.error("Error in output node: {}", e.getMessage(), e);
            return NodeExecutionResult.failure("Output operation failed: " + e.getMessage());
        }
    }
    
    private NodeExecutionResult saveToStorage(WorkflowNode node, NodeExecutionContext context, 
                                             Map<String, Object> config, Object data) {
        log.info("Saving output to storage");
        
        String destination = (String) config.getOrDefault("destination", "/workflow-outputs");
        String format = (String) config.getOrDefault("format", "json");
        
        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get("upload-dir", destination);
            Files.createDirectories(outputPath);
            
            // Generate filename
            String filename = String.format("workflow_%s_execution_%s_%s.%s",
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    LocalDateTime.now().toString().replace(":", "-"),
                    format
            );
            
            Path filePath = outputPath.resolve(filename);
            
            // TODO: Implement actual file saving based on format
            // For now, just log the operation
            log.info("Would save to: {}", filePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("outputType", "storage");
            result.put("destination", destination);
            result.put("format", format);
            result.put("filename", filename);
            result.put("path", filePath.toString());
            result.put("size", "N/A");
            result.put("status", "saved");
            
            NodeExecutionResult executionResult = NodeExecutionResult.success(result, 
                    "Output saved to storage successfully");
            executionResult.addMetadata("path", filePath.toString());
            
            return executionResult;
            
        } catch (Exception e) {
            log.error("Error saving to storage: {}", e.getMessage(), e);
            return NodeExecutionResult.failure("Failed to save to storage: " + e.getMessage());
        }
    }
    
    private NodeExecutionResult saveToProject(WorkflowNode node, NodeExecutionContext context,
                                             Map<String, Object> config, Object data) {
        log.info("Saving output to project: {}", context.getProjectId());
        
        String resourceType = (String) config.getOrDefault("resourceType", "workflow_result");
        
        // TODO: Implement actual project save via project service
        Map<String, Object> result = new HashMap<>();
        result.put("outputType", "project");
        result.put("projectId", context.getProjectId());
        result.put("resourceType", resourceType);
        result.put("resourceId", "res_" + System.currentTimeMillis());
        result.put("status", "saved");
        
        return NodeExecutionResult.success(result, "Output saved to project successfully");
    }
    
    private NodeExecutionResult sendNotification(WorkflowNode node, NodeExecutionContext context,
                                                Map<String, Object> config, Object data) {
        log.info("Sending notification");
        
        String notificationType = (String) config.getOrDefault("notificationType", "email");
        String recipient = (String) config.getOrDefault("recipient", context.getUserId());
        String message = (String) config.getOrDefault("message", "Workflow execution completed");
        
        // TODO: Implement actual notification sending
        Map<String, Object> result = new HashMap<>();
        result.put("outputType", "notification");
        result.put("notificationType", notificationType);
        result.put("recipient", recipient);
        result.put("message", message);
        result.put("sentAt", LocalDateTime.now().toString());
        result.put("status", "sent");
        
        return NodeExecutionResult.success(result, "Notification sent successfully");
    }
    
    private NodeExecutionResult generateReport(WorkflowNode node, NodeExecutionContext context,
                                              Map<String, Object> config, Object data) {
        log.info("Generating report");
        
        String reportType = (String) config.getOrDefault("reportType", "summary");
        String format = (String) config.getOrDefault("format", "pdf");
        
        // TODO: Implement actual report generation
        Map<String, Object> result = new HashMap<>();
        result.put("outputType", "report");
        result.put("reportType", reportType);
        result.put("format", format);
        result.put("reportId", "report_" + System.currentTimeMillis());
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("status", "generated");
        
        return NodeExecutionResult.success(result, "Report generated successfully");
    }
    
    private Object getPreviousNodeOutput(NodeExecutionContext context, WorkflowNode currentNode) {
        // Collect all outputs from previous nodes
        Map<String, Object> allOutputs = new HashMap<>(context.getNodeOutputs());
        
        // If there's only one output, return it directly
        if (allOutputs.size() == 1) {
            return allOutputs.values().iterator().next();
        }
        
        // Otherwise return all outputs
        return allOutputs;
    }
    
    @Override
    public boolean validate(WorkflowNode node) {
        Map<String, Object> config = node.getData().getConfig();
        String outputType = (String) config.get("outputType");
        
        if (outputType == null) {
            return false;
        }
        
        switch (outputType) {
            case "storage":
                return config.containsKey("destination");
            case "notification":
                return config.containsKey("message");
            case "report":
                return config.containsKey("reportType");
            case "project":
                return true; // Project ID comes from context
            default:
                return false;
        }
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return new NodeMetadata(
                "output",
                "Output Node",
                "Saves workflow results to storage, project, or sends notifications",
                "Output"
        );
    }
}
