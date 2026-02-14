package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.*;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for translating workflow definitions to Conductor task format
 */
@Slf4j
@Service
public class WorkflowTranslationService {
    
    /**
     * Translate WorkflowNode to Conductor WorkflowTask
     */
    public WorkflowTask translateNode(WorkflowNode node, List<WorkflowEdge> edges) {
        log.debug("Translating node: {} (type: {})", node.getName(), node.getType());
        
        WorkflowTask task = new WorkflowTask();
        
        // Set task reference name (unique identifier)
        task.setTaskReferenceName(node.getTaskReferenceName() != null 
                ? node.getTaskReferenceName() 
                : generateTaskReferenceName(node));
        
        // Set task name (task definition name in Conductor)
        NodeConfiguration config = node.getConfiguration();
        if (config != null && config.getTaskName() != null) {
            task.setName(config.getTaskName());
        } else {
            task.setName(node.getName().toLowerCase().replaceAll("\\s+", "_"));
        }
        
        // Set task type
        task.setType(translateTaskType(node.getType(), config));
        
        // Set input parameters
        Map<String, Object> inputParams = new HashMap<>();
        if (node.getInputParameters() != null) {
            inputParams.putAll(node.getInputParameters());
        }
        task.setInputParameters(inputParams);
        
        // Set optional configuration
        if (config != null) {
            // Timeout
            if (config.getTimeoutSeconds() != null) {
                task.setStartDelay(0);
                Map<String, Object> taskDef = new HashMap<>();
                taskDef.put("timeoutSeconds", config.getTimeoutSeconds());
            }
            
            // Retry configuration
            if (config.getRetryConfig() != null) {
                RetryConfig retry = config.getRetryConfig();
                task.setRetryCount(retry.getRetryCount() != null ? retry.getRetryCount() : 3);
            }
            
            // Async complete
            if (config.getAsyncComplete() != null) {
                task.setAsyncComplete(config.getAsyncComplete());
            }
        }
        
        // Optional: Set to true if task should not fail the workflow
        task.setOptional(false);
        
        return task;
    }
    
    /**
     * Translate task type from custom format to Conductor format
     */
    private String translateTaskType(String nodeType, NodeConfiguration config) {
        if (config != null && config.getTaskType() != null) {
            return config.getTaskType();
        }
        
        // Map custom types to Conductor types
        switch (nodeType.toUpperCase()) {
            case "TRIGGER":
            case "TASK":
                return "SIMPLE";
            case "DECISION":
                return "SWITCH";
            case "FORK_JOIN":
                return "FORK_JOIN";
            case "SUB_WORKFLOW":
                return "SUB_WORKFLOW";
            case "HTTP":
                return "HTTP";
            case "WAIT":
                return "WAIT";
            default:
                return "SIMPLE";
        }
    }
    
    /**
     * Build task dependencies from edges
     */
    public Map<String, List<String>> buildTaskDependencies(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        for (WorkflowEdge edge : edges) {
            String targetTaskRef = getTaskRefName(nodes, edge.getTargetNodeId());
            String sourceTaskRef = getTaskRefName(nodes, edge.getSourceNodeId());
            
            if (targetTaskRef != null && sourceTaskRef != null) {
                dependencies.computeIfAbsent(targetTaskRef, k -> new ArrayList<>()).add(sourceTaskRef);
            }
        }
        
        return dependencies;
    }
    
    /**
     * Order tasks based on dependencies (topological sort)
     */
    public List<WorkflowTask> orderTasks(List<WorkflowTask> tasks, Map<String, List<String>> dependencies) {
        List<WorkflowTask> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Map<String, WorkflowTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(WorkflowTask::getTaskReferenceName, t -> t));
        
        // Find tasks with no dependencies (start nodes)
        List<WorkflowTask> startTasks = tasks.stream()
                .filter(t -> !dependencies.containsKey(t.getTaskReferenceName()) 
                        || dependencies.get(t.getTaskReferenceName()).isEmpty())
                .collect(Collectors.toList());
        
        // Add start tasks first
        for (WorkflowTask task : startTasks) {
            if (!visited.contains(task.getTaskReferenceName())) {
                visitTask(task, taskMap, dependencies, visited, ordered);
            }
        }
        
        // Add remaining tasks (if any)
        for (WorkflowTask task : tasks) {
            if (!visited.contains(task.getTaskReferenceName())) {
                visitTask(task, taskMap, dependencies, visited, ordered);
            }
        }
        
        return ordered;
    }
    
    /**
     * Recursive DFS for topological sort
     */
    private void visitTask(WorkflowTask task, Map<String, WorkflowTask> taskMap, 
                          Map<String, List<String>> dependencies, 
                          Set<String> visited, List<WorkflowTask> ordered) {
        if (visited.contains(task.getTaskReferenceName())) {
            return;
        }
        
        visited.add(task.getTaskReferenceName());
        
        // Visit dependencies first
        List<String> deps = dependencies.getOrDefault(task.getTaskReferenceName(), Collections.emptyList());
        for (String depRef : deps) {
            WorkflowTask depTask = taskMap.get(depRef);
            if (depTask != null && !visited.contains(depRef)) {
                visitTask(depTask, taskMap, dependencies, visited, ordered);
            }
        }
        
        ordered.add(task);
    }
    
    /**
     * Get task reference name for a node ID
     */
    private String getTaskRefName(List<WorkflowNode> nodes, String nodeId) {
        return nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .map(n -> n.getTaskReferenceName() != null 
                        ? n.getTaskReferenceName() 
                        : generateTaskReferenceName(n))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Generate task reference name from node
     */
    private String generateTaskReferenceName(WorkflowNode node) {
        return node.getName().toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "") + "_" + node.getId().substring(0, 8);
    }
}
