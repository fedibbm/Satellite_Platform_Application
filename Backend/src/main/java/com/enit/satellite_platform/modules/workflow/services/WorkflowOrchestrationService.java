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
            executionService.updateExecutionStatus(execution.getId(), WorkflowExecutionStatus.RUNNING.getValue());
            executionService.addLog(execution.getId(), null, "INFO", "Workflow execution started");
            
            // Execute workflow
            Map<String, Object> context = new HashMap<>(parameters != null ? parameters : new HashMap<>());
            executeNodes(execution.getId(), version, context);
            
            // Mark as completed
            executionService.updateExecutionStatus(execution.getId(), WorkflowExecutionStatus.COMPLETED.getValue());
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
        
        // Phase 3: Step 1 - Validate DAG
        executionService.addLog(executionId, null, "INFO", "Step 1: Validating workflow DAG structure");
        validateDAG(nodes, edges, executionId);
        executionService.addLog(executionId, null, "INFO", "DAG validation completed successfully");
        
        // Find trigger node (entry point)
        WorkflowNode triggerNode = nodes.stream()
                .filter(node -> node.getType() == WorkflowNodeType.TRIGGER)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No trigger node found"));
        
        // Phase 3: Step 2 - Topological sort
        executionService.addLog(executionId, null, "INFO", "Step 2: Computing execution order (topological sort)");
        List<WorkflowNode> executionOrder = topologicalSort(nodes, edges);
        executionService.addLog(executionId, null, "INFO", 
                String.format("Execution order computed: %d nodes to execute", executionOrder.size()));
        
        // Create execution context
        NodeExecutionContext executionContext = new NodeExecutionContext(
                executionId,
                workflowId,
                userId,
                new HashMap<>(),
                new HashMap<>(),
                context.getOrDefault("projectId", "").toString(),
                context != null ? context : new HashMap<>()
        );
        
        // Phase 3: Step 3 & 4 - Execute nodes in order with data passing
        executionService.addLog(executionId, null, "INFO", "Step 3: Starting sequential node execution");
        for (WorkflowNode node : executionOrder) {
            try {
                // Phase 3: Step 5 - Update execution status
                executionService.addLog(executionId, node.getId(), "INFO", 
                        String.format("Node [%s] status: RUNNING - %s", 
                                node.getData().getLabel(), 
                                node.getType()));
                
                // Phase 3: Step 4 - Pass data via edges (prepare node inputs from predecessors)
                Map<String, Object> nodeInputs = prepareNodeInputs(node, edges, executionContext);
                executionContext.getExecutionParameters().put("nodeInputs", nodeInputs);
                
                // Execute node using executor registry
                NodeExecutionResult result = executeNodeWithExecutor(node, executionContext);
                
                if (result.isSuccess()) {
                    // Phase 3: Step 4 - Store node output in context for edge data passing
                    executionContext.setNodeOutput(node.getId(), result.getData());
                    
                    // Phase 3: Step 5 & 6 - Update execution status and store logs
                    executionService.addLog(executionId, node.getId(), "INFO", 
                            String.format("Node [%s] status: COMPLETED - %s", 
                                    node.getData().getLabel(), 
                                    result.getMessage()));
                    
                    // Log output data summary
                    if (result.getData() instanceof Map) {
                        executionService.addLog(executionId, node.getId(), "DEBUG", 
                                String.format("Node output keys: %s", ((Map<?, ?>) result.getData()).keySet()));
                    }
                    
                    // Add warnings if any
                    for (String warning : result.getWarnings()) {
                        executionService.addLog(executionId, node.getId(), "WARN", warning);
                    }
                    
                    // Check for decision nodes
                    if (node.getType() == WorkflowNodeType.DECISION) {
                        boolean condition = (boolean) result.getMetadata().getOrDefault("conditionMet", true);
                        if (!condition) {
                            executionService.addLog(executionId, node.getId(), "INFO", 
                                    "Decision condition not met, skipping downstream nodes");
                            break;
                        }
                    }
                    
                } else {
                    // Phase 3: Step 5 & 6 - Node execution failed, update status and log error
                    String errorMsg = result.getErrors().isEmpty() ? "Unknown error" : result.getErrors().get(0);
                    executionService.addLog(executionId, node.getId(), "ERROR", 
                            String.format("Node [%s] status: FAILED - %s", 
                                    node.getData().getLabel(), 
                                    errorMsg));
                    throw new RuntimeException("Node execution failed: " + errorMsg);
                }
                
            } catch (Exception e) {
                // Phase 3: Step 5 & 6 - Update execution status and store error logs
                executionService.addLog(executionId, node.getId(), "ERROR", 
                        String.format("Node [%s] status: FAILED - %s", 
                                node.getData().getLabel(), 
                                e.getMessage()));
                throw new RuntimeException("Node execution failed: " + node.getId(), e);
            }
        }
        
        // Phase 3: Step 6 - Store final execution logs
        executionService.addLog(executionId, null, "INFO", 
                String.format("Sequential execution completed: %d nodes executed successfully", 
                        executionOrder.size()));
        
        // Store final outputs in context
        context.put("nodeOutputs", executionContext.getNodeOutputs());
    }
    
    /**
     * Phase 3: Step 4 - Prepare node inputs by collecting outputs from predecessor nodes via edges
     */
    private Map<String, Object> prepareNodeInputs(WorkflowNode node, List<WorkflowEdge> edges, 
                                                   NodeExecutionContext context) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Find all edges targeting this node
        List<WorkflowEdge> incomingEdges = edges.stream()
                .filter(edge -> edge.getTarget().equals(node.getId()))
                .toList();
        
        // Collect outputs from source nodes
        for (WorkflowEdge edge : incomingEdges) {
            String sourceNodeId = edge.getSource();
            Object sourceOutput = context.getNodeOutputs().get(sourceNodeId);
            
            if (sourceOutput instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outputMap = (Map<String, Object>) sourceOutput;
                // Pass data from source to target
                String edgeLabel = edge.getLabel() != null ? edge.getLabel() : "default";
                inputs.put("from_" + sourceNodeId, outputMap);
                inputs.put(edgeLabel, outputMap);
            }
        }
        
        return inputs;
    }
    
    /**
     * Phase 3: Step 1 - Validate DAG structure
     * Checks for:
     * - Cycles (must be acyclic)
     * - Orphaned nodes (nodes with no connections)
     * - Invalid edge references
     * - Multiple trigger nodes
     */
    private void validateDAG(List<WorkflowNode> nodes, List<WorkflowEdge> edges, String executionId) {
        // Check 1: At least one node exists
        if (nodes == null || nodes.isEmpty()) {
            String error = "Validation failed: Workflow must contain at least one node";
            executionService.addLog(executionId, null, "ERROR", error);
            throw new RuntimeException(error);
        }
        
        // Check 2: Exactly one trigger node
        long triggerCount = nodes.stream()
                .filter(node -> node.getType() == WorkflowNodeType.TRIGGER)
                .count();
        
        if (triggerCount == 0) {
            String error = "Validation failed: Workflow must have exactly one trigger node (found 0)";
            executionService.addLog(executionId, null, "ERROR", error);
            throw new RuntimeException(error);
        }
        
        if (triggerCount > 1) {
            String error = String.format("Validation failed: Workflow must have exactly one trigger node (found %d)", triggerCount);
            executionService.addLog(executionId, null, "ERROR", error);
            throw new RuntimeException(error);
        }
        
        // Check 3: All edge references are valid
        Set<String> nodeIds = nodes.stream()
                .map(WorkflowNode::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        for (WorkflowEdge edge : edges) {
            if (!nodeIds.contains(edge.getSource())) {
                String error = String.format("Validation failed: Edge references non-existent source node: %s", edge.getSource());
                executionService.addLog(executionId, null, "ERROR", error);
                throw new RuntimeException(error);
            }
            
            if (!nodeIds.contains(edge.getTarget())) {
                String error = String.format("Validation failed: Edge references non-existent target node: %s", edge.getTarget());
                executionService.addLog(executionId, null, "ERROR", error);
                throw new RuntimeException(error);
            }
            
            if (edge.getSource().equals(edge.getTarget())) {
                String error = String.format("Validation failed: Self-loop detected on node: %s", edge.getSource());
                executionService.addLog(executionId, null, "ERROR", error);
                throw new RuntimeException(error);
            }
        }
        
        // Check 4: Detect cycles using DFS
        if (hasCycle(nodes, edges)) {
            String error = "Validation failed: Workflow contains cycles (must be a Directed Acyclic Graph)";
            executionService.addLog(executionId, null, "ERROR", error);
            throw new RuntimeException(error);
        }
        
        executionService.addLog(executionId, null, "DEBUG", 
                String.format("DAG validation passed: %d nodes, %d edges", nodes.size(), edges.size()));
    }
    
    /**
     * Phase 3: Step 1 - Cycle detection using DFS
     */
    private boolean hasCycle(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        // Build adjacency list
        Map<String, List<String>> graph = new HashMap<>();
        for (WorkflowNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }
        
        for (WorkflowEdge edge : edges) {
            graph.get(edge.getSource()).add(edge.getTarget());
        }
        
        // DFS with three states: white (unvisited), gray (visiting), black (visited)
        Set<String> white = new HashSet<>(graph.keySet());
        Set<String> gray = new HashSet<>();
        Set<String> black = new HashSet<>();
        
        while (!white.isEmpty()) {
            String current = white.iterator().next();
            if (hasCycleDFS(current, graph, white, gray, black)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCycleDFS(String node, Map<String, List<String>> graph, 
                                Set<String> white, Set<String> gray, Set<String> black) {
        // Move node from white to gray
        white.remove(node);
        gray.add(node);
        
        // Visit all neighbors
        for (String neighbor : graph.get(node)) {
            if (black.contains(neighbor)) {
                continue; // Already fully explored
            }
            
            if (gray.contains(neighbor)) {
                return true; // Back edge found - cycle detected
            }
            
            if (hasCycleDFS(neighbor, graph, white, gray, black)) {
                return true;
            }
        }
        
        // Move node from gray to black
        gray.remove(node);
        black.add(node);
        return false;
    }
    
    private NodeExecutionResult executeNodeWithExecutor(WorkflowNode node, NodeExecutionContext context) {
        // Get executor for this node type (convert enum to string)
        WorkflowNodeExecutor executor = nodeExecutorRegistry.getExecutor(node.getType().getValue());
        
        if (executor == null) {
            return NodeExecutionResult.failure("No executor found for node type: " + node.getType().getValue());
        }
        
        // Validate node configuration
        if (!executor.validate(node)) {
            return NodeExecutionResult.failure("Node validation failed: invalid configuration");
        }
        
        // Execute node
        return executor.execute(node, context);
    }
    
    /**
     * Phase 3: Step 2 - Topological sort using Kahn's algorithm
     * Returns nodes in execution order for sequential processing
     */
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
        
        // Phase 3: Enhanced validation - check if all nodes were sorted
        if (sortedIds.size() != nodes.size()) {
            throw new RuntimeException(
                    String.format("Topological sort failed: Expected %d nodes, but sorted only %d. " +
                            "This indicates a cycle in the workflow.", nodes.size(), sortedIds.size()));
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
