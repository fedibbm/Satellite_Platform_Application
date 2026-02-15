package com.enit.satellite_platform.modules.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for task retry policies
 * Defines retry strategies for different task types with exponential backoff
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "conductor.retry")
public class RetryPolicyConfig {

    /**
     * Default retry policy applied to all tasks unless overridden
     */
    private RetryPolicy defaultPolicy = new RetryPolicy();

    /**
     * Task-specific retry policies keyed by task type
     */
    private Map<String, RetryPolicy> policies = new HashMap<>();

    /**
     * Initialize default policies for known task types
     */
    public RetryPolicyConfig() {
        // GEE Data Worker - External API calls, more retries
        RetryPolicy geePolicy = new RetryPolicy();
        geePolicy.setMaxAttempts(5);
        geePolicy.setInitialDelayMs(2000L);
        geePolicy.setMultiplier(2.0);
        geePolicy.setMaxDelayMs(30000L);
        geePolicy.setRetryableExceptions(new String[]{"java.net.SocketTimeoutException", "java.net.ConnectException", "org.springframework.web.client.ResourceAccessException"});
        policies.put("load_image", geePolicy);

        // NDVI Processing Worker - Computation task, fewer retries
        RetryPolicy ndviPolicy = new RetryPolicy();
        ndviPolicy.setMaxAttempts(3);
        ndviPolicy.setInitialDelayMs(1000L);
        ndviPolicy.setMultiplier(1.5);
        ndviPolicy.setMaxDelayMs(10000L);
        ndviPolicy.setRetryableExceptions(new String[]{"java.net.SocketTimeoutException", "org.springframework.web.client.ResourceAccessException"});
        policies.put("calculate_ndvi", ndviPolicy);

        // Storage Worker - I/O operations, moderate retries
        RetryPolicy storagePolicy = new RetryPolicy();
        storagePolicy.setMaxAttempts(4);
        storagePolicy.setInitialDelayMs(500L);
        storagePolicy.setMultiplier(2.0);
        storagePolicy.setMaxDelayMs(8000L);
        storagePolicy.setRetryableExceptions(new String[]{"java.io.IOException", "java.nio.file.FileSystemException"});
        policies.put("save_results", storagePolicy);

        // Trigger Worker - Fast fail, minimal retries
        RetryPolicy triggerPolicy = new RetryPolicy();
        triggerPolicy.setMaxAttempts(2);
        triggerPolicy.setInitialDelayMs(500L);
        triggerPolicy.setMultiplier(1.0);
        triggerPolicy.setMaxDelayMs(1000L);
        triggerPolicy.setRetryableExceptions(new String[]{});
        policies.put("workflow_trigger", triggerPolicy);
    }

    /**
     * Get retry policy for a specific task type
     * @param taskType the task type name
     * @return the retry policy, or default if not found
     */
    public RetryPolicy getPolicyForTask(String taskType) {
        return policies.getOrDefault(taskType, defaultPolicy);
    }

    /**
     * Retry policy definition
     */
    @Data
    public static class RetryPolicy {
        /**
         * Maximum number of retry attempts
         */
        private int maxAttempts = 3;

        /**
         * Initial delay before first retry in milliseconds
         */
        private long initialDelayMs = 1000L;

        /**
         * Multiplier for exponential backoff
         */
        private double multiplier = 2.0;

        /**
         * Maximum delay between retries in milliseconds
         */
        private long maxDelayMs = 15000L;

        /**
         * Retry strategy: EXPONENTIAL, LINEAR, FIXED
         */
        private RetryStrategy strategy = RetryStrategy.EXPONENTIAL;

        /**
         * List of exception class names that trigger retries
         * Empty array means retry on all exceptions
         */
        private String[] retryableExceptions = new String[]{};

        /**
         * Calculate delay for a specific retry attempt
         * @param attemptNumber the current attempt number (1-based)
         * @return delay in milliseconds
         */
        public long calculateDelay(int attemptNumber) {
            if (attemptNumber <= 0) {
                return 0L;
            }

            long delay;
            switch (strategy) {
                case EXPONENTIAL:
                    delay = (long) (initialDelayMs * Math.pow(multiplier, attemptNumber - 1));
                    break;
                case LINEAR:
                    delay = initialDelayMs * attemptNumber;
                    break;
                case FIXED:
                default:
                    delay = initialDelayMs;
                    break;
            }

            return Math.min(delay, maxDelayMs);
        }

        /**
         * Check if an exception should trigger a retry
         * @param exception the exception to check
         * @return true if the exception is retryable
         */
        public boolean isRetryableException(Throwable exception) {
            if (retryableExceptions == null || retryableExceptions.length == 0) {
                // Retry on all exceptions if none specified
                return true;
            }

            String exceptionClassName = exception.getClass().getName();
            for (String retryableException : retryableExceptions) {
                if (exceptionClassName.equals(retryableException) || 
                    exception.getClass().getSimpleName().equals(retryableException)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Retry strategy enum
     */
    public enum RetryStrategy {
        /**
         * Exponential backoff: delay = initialDelay * (multiplier ^ attemptNumber)
         */
        EXPONENTIAL,

        /**
         * Linear backoff: delay = initialDelay * attemptNumber
         */
        LINEAR,

        /**
         * Fixed delay: delay = initialDelay (constant)
         */
        FIXED
    }
}
