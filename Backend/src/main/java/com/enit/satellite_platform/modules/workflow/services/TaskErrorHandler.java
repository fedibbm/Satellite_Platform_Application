package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.config.RetryPolicyConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for handling task errors, tracking failures, and managing error recovery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskErrorHandler {

    private final RetryPolicyConfig retryPolicyConfig;

    // Error statistics tracking
    private final Map<String, TaskErrorStats> errorStats = new ConcurrentHashMap<>();
    private final Map<String, List<ErrorRecord>> recentErrors = new ConcurrentHashMap<>();

    /**
     * Handle a task error with retry logic
     * @param taskType the type of task that failed
     * @param taskId the unique task ID
     * @param workflowId the workflow execution ID
     * @param attemptNumber the current attempt number (1-based)
     * @param exception the exception that occurred
     * @return ErrorHandlingResult with retry decision and delay
     */
    public ErrorHandlingResult handleError(String taskType, String taskId, String workflowId, 
                                           int attemptNumber, Throwable exception) {
        log.error("Task error occurred - TaskType: {}, TaskId: {}, WorkflowId: {}, Attempt: {}, Error: {}", 
                  taskType, taskId, workflowId, attemptNumber, exception.getMessage(), exception);

        // Get retry policy for this task type
        RetryPolicyConfig.RetryPolicy policy = retryPolicyConfig.getPolicyForTask(taskType);

        // Record the error
        recordError(taskType, taskId, workflowId, attemptNumber, exception);

        // Update statistics
        updateErrorStats(taskType, exception);

        // Determine if we should retry
        boolean shouldRetry = false;
        long retryDelayMs = 0L;

        if (attemptNumber < policy.getMaxAttempts() && policy.isRetryableException(exception)) {
            shouldRetry = true;
            retryDelayMs = policy.calculateDelay(attemptNumber);
            
            log.info("Will retry task - TaskType: {}, TaskId: {}, Attempt: {}/{}, Delay: {}ms", 
                     taskType, taskId, attemptNumber, policy.getMaxAttempts(), retryDelayMs);
        } else {
            log.error("Task failed permanently - TaskType: {}, TaskId: {}, FinalAttempt: {}, Reason: {}", 
                      taskType, taskId, attemptNumber, 
                      attemptNumber >= policy.getMaxAttempts() ? "Max attempts reached" : "Non-retryable exception");
        }

        return ErrorHandlingResult.builder()
                .shouldRetry(shouldRetry)
                .retryDelayMs(retryDelayMs)
                .attemptNumber(attemptNumber)
                .maxAttempts(policy.getMaxAttempts())
                .errorMessage(exception.getMessage())
                .exceptionType(exception.getClass().getSimpleName())
                .build();
    }

    /**
     * Record error for tracking and analysis
     */
    private void recordError(String taskType, String taskId, String workflowId, 
                            int attemptNumber, Throwable exception) {
        ErrorRecord record = ErrorRecord.builder()
                .taskType(taskType)
                .taskId(taskId)
                .workflowId(workflowId)
                .attemptNumber(attemptNumber)
                .timestamp(Instant.now())
                .errorMessage(exception.getMessage())
                .exceptionType(exception.getClass().getName())
                .stackTrace(getStackTraceAsString(exception))
                .build();

        recentErrors.computeIfAbsent(taskType, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(record);

        // Keep only last 100 errors per task type
        List<ErrorRecord> errors = recentErrors.get(taskType);
        synchronized (errors) {
            if (errors.size() > 100) {
                errors.remove(0);
            }
        }
    }

    /**
     * Update error statistics
     */
    private void updateErrorStats(String taskType, Throwable exception) {
        TaskErrorStats stats = errorStats.computeIfAbsent(taskType, k -> new TaskErrorStats(taskType));
        stats.incrementTotalErrors();
        stats.incrementExceptionType(exception.getClass().getSimpleName());
        stats.updateLastError(Instant.now());
    }

    /**
     * Get error statistics for a task type
     */
    public TaskErrorStats getErrorStats(String taskType) {
        return errorStats.get(taskType);
    }

    /**
     * Get all error statistics
     */
    public Map<String, TaskErrorStats> getAllErrorStats() {
        return new HashMap<>(errorStats);
    }

    /**
     * Get recent errors for a task type
     */
    public List<ErrorRecord> getRecentErrors(String taskType, int limit) {
        List<ErrorRecord> errors = recentErrors.get(taskType);
        if (errors == null) {
            return Collections.emptyList();
        }
        
        synchronized (errors) {
            int size = errors.size();
            int fromIndex = Math.max(0, size - limit);
            return new ArrayList<>(errors.subList(fromIndex, size));
        }
    }

    /**
     * Get recent errors across all task types
     */
    public List<ErrorRecord> getAllRecentErrors(int limit) {
        List<ErrorRecord> allErrors = new ArrayList<>();
        for (List<ErrorRecord> errors : recentErrors.values()) {
            synchronized (errors) {
                allErrors.addAll(errors);
            }
        }
        
        allErrors.sort(Comparator.comparing(ErrorRecord::getTimestamp).reversed());
        return allErrors.subList(0, Math.min(limit, allErrors.size()));
    }

    /**
     * Clear error statistics (useful for testing)
     */
    public void clearStats() {
        errorStats.clear();
        recentErrors.clear();
    }

    /**
     * Get stack trace as string
     */
    private String getStackTraceAsString(Throwable exception) {
        if (exception == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
            sb.append("\tat ").append(stackTrace[i].toString()).append("\n");
        }
        
        if (stackTrace.length > 10) {
            sb.append("\t... ").append(stackTrace.length - 10).append(" more\n");
        }
        
        return sb.toString();
    }

    /**
     * Result of error handling
     */
    @Data
    @lombok.Builder
    public static class ErrorHandlingResult {
        private boolean shouldRetry;
        private long retryDelayMs;
        private int attemptNumber;
        private int maxAttempts;
        private String errorMessage;
        private String exceptionType;
    }

    /**
     * Error statistics for a task type
     */
    @Data
    public static class TaskErrorStats {
        private final String taskType;
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final Map<String, AtomicLong> exceptionCounts = new ConcurrentHashMap<>();
        private Instant lastError;
        private Instant firstError;

        public TaskErrorStats(String taskType) {
            this.taskType = taskType;
        }

        public void incrementTotalErrors() {
            if (firstError == null) {
                firstError = Instant.now();
            }
            totalErrors.incrementAndGet();
        }

        public void incrementExceptionType(String exceptionType) {
            exceptionCounts.computeIfAbsent(exceptionType, k -> new AtomicLong(0))
                          .incrementAndGet();
        }

        public void updateLastError(Instant timestamp) {
            this.lastError = timestamp;
        }

        public long getTotalErrorCount() {
            return totalErrors.get();
        }

        public Map<String, Long> getExceptionTypeCounts() {
            Map<String, Long> counts = new HashMap<>();
            exceptionCounts.forEach((k, v) -> counts.put(k, v.get()));
            return counts;
        }
    }

    /**
     * Record of a single error occurrence
     */
    @Data
    @lombok.Builder
    public static class ErrorRecord {
        private String taskType;
        private String taskId;
        private String workflowId;
        private int attemptNumber;
        private Instant timestamp;
        private String errorMessage;
        private String exceptionType;
        private String stackTrace;
    }
}
