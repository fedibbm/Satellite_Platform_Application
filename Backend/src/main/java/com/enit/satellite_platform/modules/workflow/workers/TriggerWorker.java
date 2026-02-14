package com.enit.satellite_platform.modules.workflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Worker for workflow_trigger tasks.
 * Initiates workflow execution and validates input parameters.
 */
@Slf4j
@Component
public class TriggerWorker extends BaseTaskWorker {

    @Override
    public String getTaskDefName() {
        return "workflow_trigger";
    }

    @Override
    protected Map<String, Object> executeTask(Task task) throws Exception {
        log.info("TriggerWorker: Starting workflow execution");
        
        // Get input parameters (these come from workflow input)
        String workflowId = (String) getOptionalInput(task, "workflowId", "unknown");
        String userId = (String) getOptionalInput(task, "userId", "system");
        String projectId = (String) getOptionalInput(task, "projectId", "default");
        
        log.info("TriggerWorker: Workflow={}, User={}, Project={}", 
            workflowId, userId, projectId);
        
        // Create output with trigger metadata
        Map<String, Object> output = createOutput();
        output.put("triggeredAt", Instant.now().toString());
        output.put("triggeredBy", userId);
        output.put("workflowId", workflowId);
        output.put("projectId", projectId);
        output.put("status", "triggered");
        
        log.info("TriggerWorker: Workflow triggered successfully");
        return output;
    }

    @Override
    protected void validateInput(Task task) throws IllegalArgumentException {
        super.validateInput(task);
        // Trigger tasks don't have strict requirements
        // They mainly pass through workflow-level inputs
    }
}
