package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry Configuration
 * Defines retry behavior for failed tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {
    
    private Integer retryCount;
    
    private Integer retryDelaySeconds;
    
    private String retryLogic; // FIXED, EXPONENTIAL_BACKOFF, LINEAR_BACKOFF
    
    private Integer backoffScaleFactor;
}
