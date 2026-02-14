package com.enit.satellite_platform.modules.workflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all Conductor task workers.
 * Provides common functionality for error handling, logging, and result formatting.
 */
@Slf4j
@Component
public abstract class BaseTaskWorker {

    /**
     * Get the task type this worker handles.
     * This must match the task name in the workflow definition.
     */
    public abstract String getTaskDefName();

    /**
     * Execute the task logic.
     * Subclasses implement their specific business logic here.
     *
     * @param task The Conductor task to execute
     * @return Map of output data to be returned to the workflow
     * @throws Exception if task execution fails
     */
    protected abstract Map<String, Object> executeTask(Task task) throws Exception;

    /**
     * Main execution method called by Conductor.
     * Handles common concerns: logging, error handling, and result formatting.
     */
    public TaskResult execute(Task task) {
        log.info("Worker [{}] executing task: {} (ID: {})", 
            getTaskDefName(), task.getTaskType(), task.getTaskId());
        
        TaskResult result = new TaskResult(task);
        
        try {
            // Validate input
            validateInput(task);
            
            // Execute task logic
            Map<String, Object> output = executeTask(task);
            
            // Set success result
            result.setStatus(TaskResult.Status.COMPLETED);
            result.setOutputData(output);
            
            log.info("Worker [{}] completed task: {} successfully", 
                getTaskDefName(), task.getTaskId());
            
        } catch (IllegalArgumentException e) {
            // Input validation errors
            log.error("Worker [{}] validation failed for task {}: {}", 
                getTaskDefName(), task.getTaskId(), e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            
        } catch (Exception e) {
            // Execution errors
            log.error("Worker [{}] failed to execute task {}: {}", 
                getTaskDefName(), task.getTaskId(), e.getMessage(), e);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Task execution failed: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Validate task input parameters.
     * Override this method to add custom validation logic.
     */
    protected void validateInput(Task task) throws IllegalArgumentException {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getInputData() == null) {
            throw new IllegalArgumentException("Task input data cannot be null");
        }
    }

    /**
     * Helper method to get required input parameter.
     * Throws exception if parameter is missing.
     */
    protected Object getRequiredInput(Task task, String paramName) {
        Object value = task.getInputData().get(paramName);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
        }
        return value;
    }

    /**
     * Helper method to get optional input parameter with default value.
     */
    protected Object getOptionalInput(Task task, String paramName, Object defaultValue) {
        Object value = task.getInputData().get(paramName);
        return value != null ? value : defaultValue;
    }

    /**
     * Helper method to create output data map.
     */
    protected Map<String, Object> createOutput() {
        return new HashMap<>();
    }

    /**
     * Helper method to add output parameter.
     */
    protected Map<String, Object> addOutput(Map<String, Object> output, String key, Object value) {
        output.put(key, value);
        return output;
    }

    /**
     * Get worker polling configuration.
     * Override to customize polling behavior for specific workers.
     */
    public int getPollingInterval() {
        return 1000; // 1 second default
    }

    /**
     * Get number of threads for this worker.
     * Override to customize thread count for specific workers.
     */
    public int getThreadCount() {
        return 1; // 1 thread default
    }
}
