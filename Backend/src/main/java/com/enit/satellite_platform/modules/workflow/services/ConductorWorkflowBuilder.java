package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowNode;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builder service for creating Conductor workflow definitions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConductorWorkflowBuilder {
    
    private final WorkflowTranslationService translationService;
    
    /**
     * Build Conductor WorkflowDef from our WorkflowDefinition
     */
    public WorkflowDef buildWorkflowDef(WorkflowDefinition workflow) {
        log.info("Building Conductor workflow definition for: {}", workflow.getName());
        
        WorkflowDef workflowDef = new WorkflowDef();
        
        // Set basic metadata
        workflowDef.setName(generateWorkflowName(workflow));
        workflowDef.setDescription(workflow.getDescription());
        workflowDef.setVersion(parseVersion(workflow.getVersion()));
        
        // Set workflow timeout
        if (workflow.getMetadata() != null && workflow.getMetadata().getTimeoutSeconds() != null) {
            workflowDef.setTimeoutSeconds(workflow.getMetadata().getTimeoutSeconds().longValue());
        }
        
        // Translate nodes to tasks
        List<WorkflowTask> tasks = new ArrayList<>();
        for (WorkflowNode node : workflow.getNodes()) {
            WorkflowTask task = translationService.translateNode(node, workflow.getEdges());
            tasks.add(task);
        }
        
        // Build task dependencies
        Map<String, List<String>> dependencies = translationService.buildTaskDependencies(
                workflow.getNodes(), workflow.getEdges());
        
        // Order tasks based on dependencies
        List<WorkflowTask> orderedTasks = translationService.orderTasks(tasks, dependencies);
        
        workflowDef.setTasks(orderedTasks);
        
        // Set workflow input parameters
        workflowDef.setInputParameters(new ArrayList<>(Arrays.asList("imageId", "userId", "projectId")));
        
        // Set workflow output parameters
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("result", "${workflow.output}");
        workflowDef.setOutputParameters(outputParams);
        
        // Set schema version
        workflowDef.setSchemaVersion(2);
        
        // Set restartable flag
        if (workflow.getMetadata() != null && workflow.getMetadata().getRestartable() != null) {
            workflowDef.setRestartable(workflow.getMetadata().getRestartable());
        } else {
            workflowDef.setRestartable(true);
        }
        
        // Set owner email (optional)
        workflowDef.setOwnerEmail(workflow.getCreatedBy());
        
        log.info("Built workflow definition with {} tasks", orderedTasks.size());
        return workflowDef;
    }
    
    /**
     * Generate Conductor workflow name (must be unique)
     */
    private String generateWorkflowName(WorkflowDefinition workflow) {
        // Conductor workflow names should be lowercase with underscores
        String workflowName = workflow.getName()
                .toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
        
        // Always include project prefix to ensure uniqueness
        String projectPrefix = (workflow.getProjectId() != null && !workflow.getProjectId().isEmpty()) 
            ? workflow.getProjectId() : "default";
        
        return projectPrefix + "_" + workflowName;
    }
    
    /**
     * Parse version string to integer (Conductor uses integer versions)
     */
    private Integer parseVersion(String version) {
        if (version == null) {
            return 1;
        }
        
        try {
            // Extract major version number from "1.0", "2.1", etc.
            String[] parts = version.split("\\.");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            log.warn("Could not parse version: {}, defaulting to 1", version);
            return 1;
        }
    }
    
    /**
     * Validate workflow definition before registration
     */
    public boolean validateWorkflowDef(WorkflowDef workflowDef) {
        if (workflowDef.getName() == null || workflowDef.getName().isEmpty()) {
            log.error("Workflow name is required");
            return false;
        }
        
        if (workflowDef.getTasks() == null || workflowDef.getTasks().isEmpty()) {
            log.error("Workflow must have at least one task");
            return false;
        }
        
        // Validate all tasks have required fields
        for (WorkflowTask task : workflowDef.getTasks()) {
            if (task.getTaskReferenceName() == null || task.getTaskReferenceName().isEmpty()) {
                log.error("Task {} is missing taskReferenceName", task.getName());
                return false;
            }
            
            if (task.getName() == null || task.getName().isEmpty()) {
                log.error("Task with reference {} is missing name", task.getTaskReferenceName());
                return false;
            }
        }
        
        log.info("Workflow definition validation passed");
        return true;
    }
}
