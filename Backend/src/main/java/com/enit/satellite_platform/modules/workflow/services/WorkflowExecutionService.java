package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.WorkflowExecutionDTO;
import com.enit.satellite_platform.modules.workflow.entities.*;
import com.enit.satellite_platform.modules.workflow.execution.*;
import com.enit.satellite_platform.modules.workflow.mapper.WorkflowMapper;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowExecutionRepository;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private WorkflowMapper workflowMapper;
    
    @Autowired
    private NodeRegistry nodeRegistry;

    public WorkflowExecutionDTO executeWorkflow(String workflowId, String userEmail) {
        logger.info("Starting execution for workflow: {} by user: {}", workflowId, userEmail);

        // Fetch workflow
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        // Verify user has access
        if (!workflow.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("Access denied to workflow");
        }

        // Get current version
        WorkflowVersion currentVersion = workflow.getVersions().stream()
                .filter(v -> v.getVersion().equals(workflow.getCurrentVersion()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Current version not found"));

        // Create execution record
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setVersion(workflow.getCurrentVersion());
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution.setTriggeredBy(userEmail);
        execution.setLogs(new ArrayList<>());
        execution.setResults(new HashMap<>());

        // Add initial log
        WorkflowLog startLog = new WorkflowLog();
        startLog.setTimestamp(LocalDateTime.now());
        startLog.setNodeId("system");
        startLog.setLevel(LogLevel.INFO);
        startLog.setMessage("Workflow execution started");
        execution.getLogs().add(startLog);

        // Save execution
        WorkflowExecution savedExecution = executionRepository.save(execution);

        // Update workflow with execution reference
        workflow.getExecutionIds().add(savedExecution.getId());
        workflowRepository.save(workflow);

        // Execute workflow asynchronously (for now, just mark as completed)
        // In a real implementation, this would trigger the actual execution
        try {
            executeWorkflowNodes(savedExecution, currentVersion);
        } catch (Exception e) {
            logger.error("Error executing workflow: {}", workflowId, e);
            savedExecution.setStatus(ExecutionStatus.FAILED);
            WorkflowLog errorLog = new WorkflowLog();
            errorLog.setTimestamp(LocalDateTime.now());
            errorLog.setNodeId("system");
            errorLog.setLevel(LogLevel.ERROR);
            errorLog.setMessage("Workflow execution failed: " + e.getMessage());
            savedExecution.getLogs().add(errorLog);
            savedExecution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(savedExecution);
        }

        return workflowMapper.toExecutionDTO(savedExecution);
    }

    private void executeWorkflowNodes(WorkflowExecution execution, WorkflowVersion version) {
        logger.info("Executing workflow nodes for execution: {}", execution.getId());

        List<WorkflowNode> nodes = version.getNodes();
        List<WorkflowEdge> edges = version.getEdges();
        
        if (nodes == null || nodes.isEmpty()) {
            WorkflowLog log = new WorkflowLog();
            log.setTimestamp(LocalDateTime.now());
            log.setNodeId("system");
            log.setLevel(LogLevel.INFO);
            log.setMessage("No nodes to execute");
            execution.getLogs().add(log);
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            return;
        }

        // Create execution context
        NodeExecutionContext context = new NodeExecutionContext(
            execution.getWorkflowId(),
            execution.getId(),
            execution.getTriggeredBy(),
            new HashMap<>(),
            new HashMap<>()
        );

        // Build execution order using topological sort
        List<WorkflowNode> executionOrder;
        try {
            executionOrder = buildExecutionOrder(nodes, edges);
        } catch (Exception e) {
            logger.error("Error building execution order: {}", e.getMessage());
            markExecutionFailed(execution, "system", "Failed to build execution order: " + e.getMessage());
            return;
        }

        logger.info("Execution order established for {} nodes", executionOrder.size());

        // Execute nodes in order
        for (WorkflowNode node : executionOrder) {
            try {
                logger.info("Executing node: {} of type: {}", node.getId(), node.getType());

                // Log node start
                addLog(execution, node.getId(), LogLevel.INFO, "Starting node execution: " + node.getData().getLabel());

                // Get the executor for this node type
                NodeExecutor executor = nodeRegistry.getExecutor(node.getType())
                    .orElseThrow(() -> new RuntimeException("No executor found for node type: " + node.getType()));

                // Validate node before execution
                if (!executor.validate(node)) {
                    throw new RuntimeException("Node validation failed: " + node.getId());
                }

                // Execute the node
                NodeExecutionResult result = executor.execute(node, context);

                // Process result
                if (result.isSuccess()) {
                    // Store node output in context for subsequent nodes
                    context.getNodeOutputs().put(node.getId(), result.getData());
                    
                    // Log success
                    addLog(execution, node.getId(), LogLevel.INFO, 
                        "Node completed successfully");

                    // For decision nodes, handle conditional routing
                    if (node.getType() == NodeType.DECISION && result.getData() != null) {
                        Map<String, Object> output = (Map<String, Object>) result.getData();
                        Boolean decision = (Boolean) output.get("decision");
                        logger.info("Decision node {} returned: {}", node.getId(), decision);
                        
                        // Store decision for edge filtering
                        context.getGlobalVariables().put(node.getId() + ".decision", decision);
                    }
                } else {
                    // Node execution failed
                    String errorMsg = result.getErrors().isEmpty() ? 
                        "Node execution failed" : String.join(", ", result.getErrors());
                    throw new RuntimeException(errorMsg);
                }

                // Save execution state after each node
                executionRepository.save(execution);

            } catch (Exception e) {
                logger.error("Error executing node: {}", node.getId(), e);
                markExecutionFailed(execution, node.getId(), "Node execution error: " + e.getMessage());
                throw e;
            }
        }

        // Mark execution as completed
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        
        // Store final results
        execution.setResults(context.getNodeOutputs());

        addLog(execution, "system", LogLevel.INFO, "Workflow execution completed successfully");
        executionRepository.save(execution);
        
        logger.info("Workflow execution completed: {}", execution.getId());
    }

    /**
     * Build execution order using topological sort (Kahn's algorithm)
     */
    private List<WorkflowNode> buildExecutionOrder(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        // Create adjacency list and in-degree map
        Map<String, List<String>> adjacencyList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, WorkflowNode> nodeMap = new HashMap<>();

        // Initialize structures
        for (WorkflowNode node : nodes) {
            nodeMap.put(node.getId(), node);
            adjacencyList.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }

        // Build graph
        if (edges != null) {
            for (WorkflowEdge edge : edges) {
                adjacencyList.get(edge.getSource()).add(edge.getTarget());
                inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            }
        }

        // Find nodes with no incoming edges (start nodes)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Topological sort
        List<WorkflowNode> executionOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            executionOrder.add(nodeMap.get(nodeId));

            // Reduce in-degree for neighbors
            for (String neighbor : adjacencyList.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Check for cycles
        if (executionOrder.size() != nodes.size()) {
            throw new RuntimeException("Workflow contains a cycle - cannot execute");
        }

        logger.info("Execution order: {}", 
            executionOrder.stream().map(WorkflowNode::getId).collect(Collectors.joining(" -> ")));

        return executionOrder;
    }

    private void addLog(WorkflowExecution execution, String nodeId, LogLevel level, String message) {
        WorkflowLog log = new WorkflowLog();
        log.setTimestamp(LocalDateTime.now());
        log.setNodeId(nodeId);
        log.setLevel(level);
        log.setMessage(message);
        execution.getLogs().add(log);
    }

    private void markExecutionFailed(WorkflowExecution execution, String nodeId, String errorMessage) {
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setCompletedAt(LocalDateTime.now());
        addLog(execution, nodeId, LogLevel.ERROR, errorMessage);
        executionRepository.save(execution);
    }

    private Object executeNode(WorkflowNode node, Map<String, Object> previousOutputs) {
        // This method is no longer used - kept for backward compatibility
        // All execution now goes through NodeExecutors via executeWorkflowNodes
        logger.warn("Deprecated executeNode method called for node: {}", node.getId());
        return Map.of("executed", true, "deprecated", true);
    }

    public List<WorkflowExecutionDTO> getWorkflowExecutions(String workflowId, String userEmail) {
        logger.info("Fetching executions for workflow: {}", workflowId);

        // Verify user has access to workflow
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        if (!workflow.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("Access denied to workflow");
        }

        List<WorkflowExecution> executions = executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
        return executions.stream()
                .map(workflowMapper::toExecutionDTO)
                .toList();
    }

    public WorkflowExecutionDTO getExecutionById(String executionId, String userEmail) {
        logger.info("Fetching execution: {}", executionId);

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        // Verify user has access
        Workflow workflow = workflowRepository.findById(execution.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("Associated workflow not found"));

        if (!workflow.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("Access denied to execution");
        }

        return workflowMapper.toExecutionDTO(execution);
    }
}
