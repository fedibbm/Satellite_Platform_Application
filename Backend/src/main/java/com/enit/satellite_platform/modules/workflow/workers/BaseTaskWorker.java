package com.enit.satellite_platform.modules.workflow.workers;

import com.enit.satellite_platform.modules.workflow.config.RetryPolicyConfig;
import com.enit.satellite_platform.modules.workflow.monitoring.WorkflowLogger;
import com.enit.satellite_platform.modules.workflow.services.CompensationHandler;
import com.enit.satellite_platform.modules.workflow.services.TaskErrorHandler;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all Conductor task workers.
 * Provides common functionality for error handling, retry logic, compensation, and logging.
 */
@Slf4j
@Component
public abstract class BaseTaskWorker {

    @Autowired(required = false)
    protected TaskErrorHandler errorHandler;

    @Autowired(required = false)
    protected CompensationHandler compensationHandler;

    @Autowired(required = false)
    protected RetryPolicyConfig retryPolicyConfig;

    @Autowired(required = false)
    protected WorkerConfiguration workerConfiguration;

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
     * Handles common concerns: logging, error handling, retry logic, and result formatting.
     */
    public TaskResult execute(Task task) {
        long startTime = System.currentTimeMillis();
        String workflowId = task.getWorkflowInstanceId();
        String taskId = task.getTaskId();
        String taskType = getTaskDefName();
        int attemptNumber = task.getRetryCount() + 1;

        // Set MDC context for structured logging
        WorkflowLogger.setTaskContext(taskId, taskType, attemptNumber);
        WorkflowLogger.logTaskStart(taskType, taskId, task.getInputData());

        TaskResult result = new TaskResult(task);
        
        try {
            // Validate input
            validateInput(task);
            
            // Register compensation actions before execution
            registerCompensationActions(task);
            
            // Execute task logic with timeout monitoring
            Map<String, Object> output = executeTaskWithTimeout(task);
            
            // Set success result
            result.setStatus(TaskResult.Status.COMPLETED);
            result.setOutputData(output);
            
            long executionTime = System.currentTimeMillis() - startTime;
            WorkflowLogger.logTaskSuccess(taskType, taskId, executionTime, output);
            
            // Clear compensation actions on success
            if (compensationHandler != null) {
                compensationHandler.clearCompensation(workflowId);
            }
            
        } catch (IllegalArgumentException e) {
            // Input validation errors - don't retry
            long executionTime = System.currentTimeMillis() - startTime;
            WorkflowLogger.logTaskFailure(taskType, taskId, executionTime, e.getMessage(), e);
            
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Validation error: " + e.getMessage());
            
        } catch (Exception e) {
            // Execution errors - handle with retry logic
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (errorHandler != null) {
                TaskErrorHandler.ErrorHandlingResult errorResult = errorHandler.handleError(
                    taskType, taskId, workflowId, attemptNumber, e
                );
                
                if (errorResult.isShouldRetry()) {
                    // Retry with backoff
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Task execution failed: " + e.getMessage());
                    result.setCallbackAfterSeconds(errorResult.getRetryDelayMs() / 1000);
                    
                    WorkflowLogger.logTaskRetry(taskType, taskId, attemptNumber, 
                                              errorResult.getMaxAttempts(), errorResult.getRetryDelayMs());
                } else {
                    // Final failure - trigger compensation
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Task execution failed: " + e.getMessage());
                    
                    WorkflowLogger.logTaskFailure(taskType, taskId, executionTime, e.getMessage(), e);
                    
                    // Execute compensation
                    if (compensationHandler != null) {
                        compensationHandler.compensate(workflowId, "Task failed after " + attemptNumber + " attempts");
                    }
                }
            } else {
                // Fallback without error handler
                WorkflowLogger.logTaskFailure(taskType, taskId, executionTime, e.getMessage(), e);
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("Task execution failed: " + e.getMessage());
            }
        } finally {
            // Clear task context from MDC
            WorkflowLogger.clearTaskContext();
        }
        
        return result;
    }

    /**
     * Execute task with timeout monitoring
     */
    private Map<String, Object> executeTaskWithTimeout(Task task) throws Exception {
        // Get timeout for this task type
        int timeoutSeconds = workerConfiguration != null ? 
            workerConfiguration.getTimeoutForTask(getTaskDefName()) : 300;
        
        // Note: Conductor handles timeout at server level
        // This is just for logging and monitoring
        WorkflowLogger.PerformanceTimer timer = WorkflowLogger.startTimer("Task execution: " + getTaskDefName());
        
        try {
            Map<String, Object> result = executeTask(task);
            long durationMs = timer.stop();
            
            if (durationMs > (timeoutSeconds * 1000)) {
                log.warn("Task execution exceeded expected timeout: {}ms > {}ms", 
                        durationMs, timeoutSeconds * 1000);
            }
            
            return result;
        } catch (Exception e) {
            timer.stop();
            throw e;
        }
    }

    /**
     * Register compensation actions for this task
     * Override this method to register task-specific cleanup actions
     */
    protected void registerCompensationActions(Task task) {
        // Default: no compensation actions
        // Subclasses override to add specific compensation logic
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
