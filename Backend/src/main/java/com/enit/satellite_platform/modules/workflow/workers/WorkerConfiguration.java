package com.enit.satellite_platform.modules.workflow.workers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
}
