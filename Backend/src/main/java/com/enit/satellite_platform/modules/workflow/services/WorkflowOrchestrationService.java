package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.*;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionContext;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutionResult;
import com.enit.satellite_platform.modules.workflow.execution.NodeExecutorRegistry;
import com.enit.satellite_platform.modules.workflow.execution.WorkflowNodeExecutor;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrationService {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService executionService;
    private final NodeRegistryService nodeRegistryService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    
    @Async
    public void executeWorkflow(String workflowId, String userId, Map<String, Object> parameters) {
        log.info("Starting workflow execution for workflowId: {}", workflowId);
        
        // Get workflow
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        
        // Get current version
        WorkflowVersion version = workflow.getVersions().stream()
                .filter(v -> v.getVersion().equals(workflow.getCurrentVersion()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Current version not found"));
        
        // Create execution record
        WorkflowExecution execution = executionService.createExecution(
                workflowId, 
                version.getVersion(), 
                userId, 
                parameters
        );
        
        try {
            // Update status to running
            executionService.updateExecutionStatus(execution.getId(), "running");
            executionService.addLog(execution.getId(), null, "INFO", "Workflow execution started");
            
            // Execute workflow
            Map<String, Object> context = new HashMap<>(parameters != null ? parameters : new HashMap<>());
            executeNodes(execution.getId(), version, context);
            
            // Mark as completed
            executionService.updateExecutionStatus(execution.getId(), "completed");
            executionService.updateExecutionResult(execution.getId(), context);
            executionService.addLog(execution.getId(), null, "INFO", "Workflow execution completed successfully");
            
            log.info("Workflow execution completed successfully: {}", execution.getId());
            
        } catch (Exception e) {
            log.error("Workflow execution failed: {}", execution.getId(), e);
            executionService.setExecutionError(execution.getId(), e.getMessage());
            executionService.addLog(execution.getId(), null, "ERROR", "Workflow execution failed: " + e.getMessage());
        }
    }
    
    private void executeNodes(String executionId, WorkflowVersion version, Map<String, Object> context) {
        List<WorkflowNode> nodes = version.getNodes();
        List<WorkflowEdge> edges = version.getEdges();
        
        // Find trigger node (entry point)
        WorkflowNode triggerNode = nodes.stream()
                .filter(node -> "trigger".equals(node.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No trigger node found"));
        
        // Build execution order using topological sort
        List<WorkflowNode> executionOrder = topologicalSort(nodes, edges);
        
        // Create execution context
        NodeExecutionContext executionContext = new NodeExecutionContext(
                version.getWorkflowId(),
                executionId,
                context.getOrDefault("projectId", "").toString(),
                context.getOrDefault("userId", "").toString(),
                context
        );
        
        // Execute nodes in order
        for (WorkflowNode node : executionOrder) {
            try {
                executionService.addLog(executionId, node.getId(), "INFO", 
                        "Executing node: " + node.getData().getLabel());
                
                // Execute node using executor registry
                NodeExecutionResult result = executeNodeWithExecutor(node, executionContext);
                
                if (result.isSuccess()) {
                    // Store node output in context
                    executionContext.addNodeOutput(node.getId(), result.getData());
                    
                    executionService.addLog(executionId, node.getId(), "INFO", 
                            "Node executed successfully: " + result.getMessage());
                    
                    // Add warnings if any
                    for (String warning : result.getWarnings()) {
                        executionService.addLog(executionId, node.getId(), "WARN", warning);
                    }
                    
                    // Check for decision nodes
                    if ("decision".equals(node.getType())) {
                        boolean condition = (boolean) result.getMetadata().getOrDefault("conditionMet", true);
                        if (!condition) {
                            executionService.addLog(executionId, node.getId(), "INFO", 
                                    "Decision condition not met, skipping downstream nodes");
                            break;
                        }
                    }
                    
                } else {
                    // Node execution failed
                    executionService.addLog(executionId, node.getId(), "ERROR", 
                            "Node execution failed: " + result.getErrorMessage());
                    throw new RuntimeException("Node execution failed: " + result.getErrorMessage());
                }
                
            } catch (Exception e) {
                executionService.addLog(executionId, node.getId(), "ERROR", 
                        "Node execution failed: " + e.getMessage());
                throw new RuntimeException("Node execution failed: " + node.getId(), e);
            }
        }
        
        // Store final outputs in context
        context.put("nodeOutputs", executionContext.getNodeOutputs());
    }
    
    private NodeExecutionResult executeNodeWithExecutor(WorkflowNode node, NodeExecutionContext context) {
        // Get executor for this node type
        WorkflowNodeExecutor executor = nodeExecutorRegistry.getExecutor(node.getType());
        
        if (executor == null) {
            return NodeExecutionResult.failure("No executor found for node type: " + node.getType());
        }
        
        // Validate node configuration
        if (!executor.validate(node)) {
            return NodeExecutionResult.failure("Node validation failed: invalid configuration");
        }
        
        // Execute node
        return executor.execute(node, context);
    }
    
    
    private List<WorkflowNode> topologicalSort(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        // Build adjacency list
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        for (WorkflowNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        
        for (WorkflowEdge edge : edges) {
            graph.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }
        
        // Kahn's algorithm for topological sort
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<String> sortedIds = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sortedIds.add(nodeId);
            
            for (String neighbor : graph.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // Convert IDs back to nodes in sorted order
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        
        List<WorkflowNode> sortedNodes = new ArrayList<>();
        for (String id : sortedIds) {
            sortedNodes.add(nodeMap.get(id));
        }
        
        return sortedNodes;
    }
}
