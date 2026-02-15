package com.enit.satellite_platform.modules.workflow.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility for structured logging with MDC (Mapped Diagnostic Context)
 * Provides consistent logging format for workflow and task operations
 */
@Slf4j
public class WorkflowLogger {

    // MDC Keys
    private static final String WORKFLOW_ID = "workflowId";
    private static final String TASK_ID = "taskId";
    private static final String TASK_TYPE = "taskType";
    private static final String PROJECT_ID = "projectId";
    private static final String CORRELATION_ID = "correlationId";
    private static final String ATTEMPT_NUMBER = "attemptNumber";
    private static final String EXECUTION_TIME = "executionTimeMs";

    /**
     * Set workflow context in MDC
     */
    public static void setWorkflowContext(String workflowId, String projectId) {
        MDC.put(WORKFLOW_ID, workflowId);
        MDC.put(PROJECT_ID, projectId);
        if (MDC.get(CORRELATION_ID) == null) {
            MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
        }
    }

    /**
     * Set task context in MDC
     */
    public static void setTaskContext(String taskId, String taskType, int attemptNumber) {
        MDC.put(TASK_ID, taskId);
        MDC.put(TASK_TYPE, taskType);
        MDC.put(ATTEMPT_NUMBER, String.valueOf(attemptNumber));
    }

    /**
     * Clear all MDC context
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Clear only task context (keep workflow context)
     */
    public static void clearTaskContext() {
        MDC.remove(TASK_ID);
        MDC.remove(TASK_TYPE);
        MDC.remove(ATTEMPT_NUMBER);
        MDC.remove(EXECUTION_TIME);
    }

    /**
     * Log task start
     */
    public static void logTaskStart(String taskType, String taskId, Map<String, Object> input) {
        log.info("Task started - Type: {}, ID: {}, Input: {}", taskType, taskId, sanitizeInput(input));
    }

    /**
     * Log task completion
     */
    public static void logTaskSuccess(String taskType, String taskId, long executionTimeMs, Map<String, Object> output) {
        MDC.put(EXECUTION_TIME, String.valueOf(executionTimeMs));
        log.info("Task completed successfully - Type: {}, ID: {}, Duration: {}ms, Output: {}", 
                 taskType, taskId, executionTimeMs, sanitizeOutput(output));
    }

    /**
     * Log task failure
     */
    public static void logTaskFailure(String taskType, String taskId, long executionTimeMs, 
                                     String errorMessage, Throwable exception) {
        MDC.put(EXECUTION_TIME, String.valueOf(executionTimeMs));
        log.error("Task failed - Type: {}, ID: {}, Duration: {}ms, Error: {}", 
                  taskType, taskId, executionTimeMs, errorMessage, exception);
    }

    /**
     * Log task retry
     */
    public static void logTaskRetry(String taskType, String taskId, int attemptNumber, 
                                   int maxAttempts, long retryDelayMs) {
        log.warn("Task retry scheduled - Type: {}, ID: {}, Attempt: {}/{}, Delay: {}ms", 
                 taskType, taskId, attemptNumber, maxAttempts, retryDelayMs);
    }

    /**
     * Log workflow execution start
     */
    public static void logWorkflowStart(String workflowId, String workflowName, String projectId, 
                                       Map<String, Object> input) {
        log.info("Workflow execution started - ID: {}, Name: {}, Project: {}, Input: {}", 
                 workflowId, workflowName, projectId, sanitizeInput(input));
    }

    /**
     * Log workflow completion
     */
    public static void logWorkflowComplete(String workflowId, String status, Duration duration) {
        log.info("Workflow execution completed - ID: {}, Status: {}, Duration: {}ms", 
                 workflowId, status, duration.toMillis());
    }

    /**
     * Log compensation action
     */
    public static void logCompensation(String workflowId, String reason, int actionsCount) {
        log.warn("Compensation triggered - Workflow: {}, Reason: {}, Actions: {}", 
                 workflowId, reason, actionsCount);
    }

    /**
     * Log external API call
     */
    public static void logExternalCall(String serviceName, String endpoint, long durationMs, boolean success) {
        if (success) {
            log.debug("External API call succeeded - Service: {}, Endpoint: {}, Duration: {}ms", 
                      serviceName, endpoint, durationMs);
        } else {
            log.warn("External API call failed - Service: {}, Endpoint: {}, Duration: {}ms", 
                     serviceName, endpoint, durationMs);
        }
    }

    /**
     * Log performance metric
     */
    public static void logPerformanceMetric(String metricName, long value, String unit) {
        log.info("Performance metric - {}: {} {}", metricName, value, unit);
    }

    /**
     * Create a performance timer
     */
    public static PerformanceTimer startTimer(String operation) {
        return new PerformanceTimer(operation);
    }

    /**
     * Sanitize input for logging (remove sensitive data)
     */
    private static Map<String, Object> sanitizeInput(Map<String, Object> input) {
        if (input == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> sanitized = new HashMap<>(input);
        // Remove sensitive fields if present
        sanitized.remove("password");
        sanitized.remove("apiKey");
        sanitized.remove("token");
        sanitized.remove("secret");
        
        return sanitized;
    }

    /**
     * Sanitize output for logging
     */
    private static Map<String, Object> sanitizeOutput(Map<String, Object> output) {
        if (output == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> sanitized = new HashMap<>();
        // Log only key information, not full data
        output.forEach((key, value) -> {
            if (value instanceof String && ((String) value).length() > 100) {
                sanitized.put(key, ((String) value).substring(0, 100) + "...");
            } else {
                sanitized.put(key, value);
            }
        });
        
        return sanitized;
    }

    /**
     * Performance timer for measuring operation duration
     */
    public static class PerformanceTimer {
        private final String operation;
        private final Instant startTime;

        public PerformanceTimer(String operation) {
            this.operation = operation;
            this.startTime = Instant.now();
            log.debug("Operation started: {}", operation);
        }

        public long stop() {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            log.debug("Operation completed: {} (Duration: {}ms)", operation, durationMs);
            return durationMs;
        }

        public long stopAndLog() {
            long durationMs = stop();
            logPerformanceMetric(operation, durationMs, "ms");
            return durationMs;
        }
    }
}
