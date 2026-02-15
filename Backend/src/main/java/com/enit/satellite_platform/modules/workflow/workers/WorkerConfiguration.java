package com.enit.satellite_platform.modules.workflow.workers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Conductor workers.
 * Allows customization of worker behavior via application.properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "conductor.worker")
public class WorkerConfiguration {

    /**
     * Number of threads per worker (default: 5)
     */
    private int threadCount = 5;

    /**
     * Polling interval in milliseconds (default: 1000ms = 1s)
     */
    private int pollingInterval = 1000;

    /**
     * Worker domain for task routing (optional)
     */
    private String domain;

    /**
     * Whether to auto-start workers on application startup
     */
    private boolean autoStart = true;

    /**
     * Task update retry count
     */
    private int taskUpdateRetry = 3;

    /**
     * Worker lease time in seconds
     */
    private int leaseTime = 60;

    /**
     * Batch poll size
     */
    private int batchSize = 1;

    /**
     * Default response timeout in seconds (how long to wait for task execution)
     */
    private int responseTimeoutSeconds = 300;

    /**
     * Default poll timeout in milliseconds (how long to wait for new tasks)
     */
    private int pollTimeoutMs = 100;

    /**
     * Task-specific timeout configurations (in seconds)
     * Key: task type, Value: timeout in seconds
     */
    private Map<String, Integer> taskTimeouts = new HashMap<>();

    /**
     * Initialize default task timeouts
     */
    public WorkerConfiguration() {
        // GEE Data Worker - External API calls may take longer
        taskTimeouts.put("load_image", 180); // 3 minutes

        // NDVI Processing Worker - Computation task
        taskTimeouts.put("calculate_ndvi", 120); // 2 minutes

        // Storage Worker - I/O operations
        taskTimeouts.put("save_results", 60); // 1 minute

        // Trigger Worker - Fast operation
        taskTimeouts.put("workflow_trigger", 30); // 30 seconds
    }

    /**
     * Get timeout for a specific task type
     * @param taskType the task type
     * @return timeout in seconds
     */
    public int getTimeoutForTask(String taskType) {
        return taskTimeouts.getOrDefault(taskType, responseTimeoutSeconds);
    }
}
