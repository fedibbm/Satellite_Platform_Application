package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.WorkflowExecutionDTO;
import com.enit.satellite_platform.modules.workflow.entities.*;
import com.enit.satellite_platform.modules.workflow.execution.*;
import com.enit.satellite_platform.modules.workflow.mapper.WorkflowMapper;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowExecutionRepository;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.enit.satellite_platform.modules.workflow.config.WorkflowRabbitMQConfig;
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
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NodeRegistry nodeRegistry;

    @Autowired
    private com.enit.satellite_platform.modules.workflow.execution.graph.WorkflowValidator workflowValidator;

    @Autowired
    private com.enit.satellite_platform.modules.workflow.execution.graph.ExecutionPlanner executionPlanner;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void checkWorkflowAccess(Workflow workflow, String userEmail, PermissionLevel requiredLevel) {
        User user = getUser(userEmail);

        if (workflow.getProjectId() != null) {
            Project project = projectRepository.findById(workflow.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            if (!project.hasAccess(user, requiredLevel)) {
                throw new RuntimeException("Access denied to workflow");
            }
            return;
        }

        if (!workflow.getCreatedBy().equals(userEmail)) {
            throw new RuntimeException("Access denied to workflow");
        }
    }

    public WorkflowExecutionDTO executeWorkflow(String workflowId, String userEmail) {
        logger.info("Starting execution for workflow: {} by user: {}", workflowId, userEmail);

        // Fetch workflow
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        checkWorkflowAccess(workflow, userEmail, PermissionLevel.EDITOR);

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

        // Submit to RabbitMQ for asynchronous execution
        rabbitTemplate.convertAndSend(WorkflowRabbitMQConfig.WORKFLOW_EXECUTION_QUEUE, savedExecution.getId());
        logger.info("Workflow execution {} submitted to RabbitMQ for processing", savedExecution.getId());

        // Reload execution from database to ensure we have the latest state
        WorkflowExecution finalExecution = executionRepository.findById(savedExecution.getId())
                .orElse(savedExecution);

        logger.info("Execution {} finished with status: {}", finalExecution.getId(), finalExecution.getStatus());

        // If execution ultimately failed, surface a meaningful error instead of silently returning success
        if (finalExecution.getStatus() == ExecutionStatus.FAILED) {
            String errorMessage = "Workflow execution failed";

            if (finalExecution.getLogs() != null && !finalExecution.getLogs().isEmpty()) {
                // Find the last ERROR-level log if available
                WorkflowLog lastErrorLog = finalExecution.getLogs().stream()
                        .filter(l -> l.getLevel() == LogLevel.ERROR)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (lastErrorLog != null) {
                    errorMessage = String.format(
                            "Workflow execution failed at node '%s': %s",
                            lastErrorLog.getNodeId(),
                            lastErrorLog.getMessage()
                    );
                }
            }

            throw new RuntimeException(errorMessage);
        }

        return workflowMapper.toExecutionDTO(finalExecution);
    }

    
    @RabbitListener(queues = WorkflowRabbitMQConfig.WORKFLOW_EXECUTION_QUEUE)
    public void processWorkflowExecutionQueue(String executionId) {
        logger.info("Received executionId {} from queue", executionId);
        try {
            WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null) {
                logger.error("Execution not found: {}", executionId);
                return;
            }
            
            Workflow workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
            if (workflow == null) {
                logger.error("Workflow not found: {}", execution.getWorkflowId());
                return;
            }
            
            WorkflowVersion version = workflow.getVersions().stream()
                    .filter(v -> v.getVersion().equals(execution.getVersion()))
                    .findFirst()
                    .orElse(null);
                    
            if (version == null) {
                logger.error("Version {} not found for workflow {}", execution.getVersion(), workflow.getId());
                return;
            }
            
            executeWorkflowNodes(execution, version);
        } catch (Exception e) {
            logger.error("Failed to process execution {} from queue", executionId, e);
        }
    }

    private void executeWorkflowNodes(WorkflowExecution execution, WorkflowVersion version) {
        logger.info("Executing workflow nodes for execution: {}", execution.getId());

        Workflow workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
        String workflowProjectId = (workflow != null && workflow.getProjectId() != null) ? workflow.getProjectId().toString() : null;

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

        // Initialize global variables with projectId if available
        Map<String, Object> globalVariables = new HashMap<>();
        if (workflowProjectId != null) {
            globalVariables.put("projectId", workflowProjectId);
        }

        // Create execution context
        NodeExecutionContext context = new NodeExecutionContext(
            execution.getWorkflowId(),
            execution.getId(),
            execution.getTriggeredBy(),
            globalVariables,
            new HashMap<>()
        );

        // Validate workflow graph first
        com.enit.satellite_platform.modules.workflow.execution.graph.WorkflowValidator.ValidationResult validationResult = workflowValidator.validate(version);
        if (!validationResult.isValid()) {
            markExecutionFailed(execution, "system", "Workflow Validation Failed: " + validationResult.getErrorMessage());
            logger.error("Workflow validation failed: {}", validationResult.getErrorMessage());
            return;
        }

        // Build execution order using our new ExecutionPlanner (Topological Sort)
        List<List<WorkflowNode>> executionPlan;
        try {
            executionPlan = executionPlanner.planExecution(version);
            if (executionPlan.isEmpty()) {
                throw new RuntimeException("Execution plan generated 0 stages.");
            }
        } catch (Exception e) {
            logger.error("Error building execution order: {}", e.getMessage());
            markExecutionFailed(execution, "system", "Failed to build execution order: " + e.getMessage());
            return;
        }

        logger.info("Execution order established. Stages to execute: {}", executionPlan.size());

        // Execute node stages in order
        for (List<WorkflowNode> stage : executionPlan) {
            logger.info("Executing new stage with {} nodes", stage.size());
            
            // NOTE: Currently executing sequentially within the stage, 
            // but this loop allows for future adaptation to parallel execution via CompletableFuture
            for (WorkflowNode node : stage) {
                try {
                    // --- SKIPPING LOGIC FOR DECISION NODES ---
                    boolean shouldSkip = false;
                    
                    // If it has incoming edges, check if they are all active
                    List<com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge> inboundEdges = edges.stream()
                            .filter(e -> e.getTarget().equals(node.getId()))
                            .collect(Collectors.toList());

                    if (!inboundEdges.isEmpty()) {
                        boolean hasActiveInbound = false;
                        for (com.enit.satellite_platform.modules.workflow.entities.WorkflowEdge edge : inboundEdges) {
                            String sourceId = edge.getSource();
                            
                            // Check if source was skipped
                            Object sourceSkipped = context.getGlobalVariables().get(sourceId + ".skipped");
                            if (Boolean.TRUE.equals(sourceSkipped)) {
                                continue;
                            }
                            
                            // Check if source was a decision that evaluated differently
                            Object decisionResult = context.getGlobalVariables().get(sourceId + ".decision");
                            if (decisionResult != null) {
                                String expectedLabel = edge.getLabel() != null ? edge.getLabel().toLowerCase() : "";
                                // The Decision node returns Boolean decision. We convert "true"/"false" and match
                                String decisionStr = String.valueOf(decisionResult).toLowerCase();
                                if (!expectedLabel.isEmpty() && !expectedLabel.equals(decisionStr)) {
                                    continue;
                                }
                            }
                            
                            hasActiveInbound = true;
                            break;
                        }
                        
                        if (!hasActiveInbound) {
                            shouldSkip = true;
                        }
                    }

                    if (shouldSkip) {
                        logger.info("Skipping node {} due to conditional routing", node.getId());
                        context.getGlobalVariables().put(node.getId() + ".skipped", true);
                        addLog(execution, node.getId(), LogLevel.INFO, "Skipped node execution due to conditional routing");
                        continue;
                    }

                    notifyStatusUpdate(execution.getId(), "NODE_START", "Executing node", Map.of("nodeId", node.getId()));
                    logger.info("Executing node: {} of type: {}", node.getId(), node.getType());

                    // Log node start
                    addLog(execution, node.getId(), LogLevel.INFO, "Starting node execution: " + node.getData().getLabel());

                    // Get the executor for this node type
                    logger.debug("Looking up executor for node type: {}", node.getType());
                    NodeExecutor executor = nodeRegistry.getExecutor(node.getType())
                        .orElseThrow(() -> {
                            logger.error("No executor found for node type: {}", node.getType());
                            return new RuntimeException("No executor found for node type: " + node.getType());
                        });

                    // Validate node before execution
                    logger.debug("Validating node: {}", node.getId());
                    if (!executor.validate(node)) {
                        logger.error("Node validation failed for node: {}", node.getId());
                        throw new RuntimeException("Node validation failed: " + node.getId());
                    }
                    logger.debug("Node validation passed: {}", node.getId());

                    // Execute the node
                    logger.debug("Executing node: {} with executor: {}", node.getId(), executor.getClass().getSimpleName());
                    NodeExecutionResult result = executor.execute(node, context);
                    logger.debug("Node execution completed: {}, success: {}", node.getId(), result.isSuccess());

                    // Process result
                    if (result.isSuccess()) {
                        // Store node output in context for subsequent nodes
                        context.getNodeOutputs().put(node.getId(), result.getData());
                        
                        notifyStatusUpdate(execution.getId(), "NODE_COMPLETE", "Node completed", Map.of("nodeId", node.getId(), "result", result.getData()));
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

                } catch (Exception e) {
                    logger.error("Error executing node: {} - Type: {}, Message: {}", 
                        node.getId(), e.getClass().getSimpleName(), e.getMessage(), e);
                    String detailedError = String.format("Node execution error: %s - %s", 
                        e.getClass().getSimpleName(), e.getMessage());
                    markExecutionFailed(execution, node.getId(), detailedError);
                    logger.info("Marked execution as FAILED, returning from executeWorkflowNodes");
                    return; // Don't throw, just return after marking as failed
                }
            }
        }

        // Mark execution as completed
        logger.info("All nodes executed successfully, marking execution as COMPLETED");
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        
        // Store final results
        execution.setResults(context.getNodeOutputs());

        addLog(execution, "system", LogLevel.INFO, "Workflow execution completed successfully");
        executionRepository.save(execution);
        logger.info("Execution saved with COMPLETED status");
        
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
        executionRepository.save(execution);
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

        checkWorkflowAccess(workflow, userEmail, PermissionLevel.READ);

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

        checkWorkflowAccess(workflow, userEmail, PermissionLevel.READ);

        return workflowMapper.toExecutionDTO(execution);
    }

    private void notifyStatusUpdate(String executionId, String status, String message, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("executionId", executionId);
            payload.put("status", status);
            payload.put("message", message);
            if (data != null) payload.put("data", data);
            
            // Broadcast to the specific workflow execution topic
            messagingTemplate.convertAndSend("/topic/workflow.execution." + executionId, payload);
        } catch (Exception e) {
            logger.warn("Failed to broadcast execution status update", e);
        }
    }

}
