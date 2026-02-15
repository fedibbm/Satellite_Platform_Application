package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.services.TaskErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for monitoring task errors, retry metrics, and failure analytics
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows/monitoring")
@RequiredArgsConstructor
public class ErrorMonitoringController {

    private final TaskErrorHandler errorHandler;

    /**
     * Get error statistics for a specific task type
     */
    @GetMapping("/errors/stats/{taskType}")
    public ResponseEntity<TaskErrorHandler.TaskErrorStats> getTaskErrorStats(
            @PathVariable String taskType) {
        log.info("GET /monitoring/errors/stats/{}", taskType);
        
        TaskErrorHandler.TaskErrorStats stats = errorHandler.getErrorStats(taskType);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get error statistics for all task types
     */
    @GetMapping("/errors/stats")
    public ResponseEntity<Map<String, TaskErrorHandler.TaskErrorStats>> getAllErrorStats() {
        log.info("GET /monitoring/errors/stats");
        
        Map<String, TaskErrorHandler.TaskErrorStats> allStats = errorHandler.getAllErrorStats();
        return ResponseEntity.ok(allStats);
    }

    /**
     * Get recent errors for a specific task type
     */
    @GetMapping("/errors/{taskType}")
    public ResponseEntity<List<TaskErrorHandler.ErrorRecord>> getRecentErrors(
            @PathVariable String taskType,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("GET /monitoring/errors/{} (limit: {})", taskType, limit);
        
        List<TaskErrorHandler.ErrorRecord> errors = errorHandler.getRecentErrors(taskType, limit);
        return ResponseEntity.ok(errors);
    }

    /**
     * Get recent errors across all task types
     */
    @GetMapping("/errors")
    public ResponseEntity<List<TaskErrorHandler.ErrorRecord>> getAllRecentErrors(
            @RequestParam(defaultValue = "100") int limit) {
        log.info("GET /monitoring/errors (limit: {})", limit);
        
        List<TaskErrorHandler.ErrorRecord> errors = errorHandler.getAllRecentErrors(limit);
        return ResponseEntity.ok(errors);
    }

    /**
     * Get error summary with aggregated metrics
     */
    @GetMapping("/errors/summary")
    public ResponseEntity<Map<String, Object>> getErrorSummary() {
        log.info("GET /monitoring/errors/summary");
        
        Map<String, TaskErrorHandler.TaskErrorStats> allStats = errorHandler.getAllErrorStats();
        
        long totalErrors = 0;
        int taskTypesWithErrors = 0;
        Map<String, Long> errorsByTaskType = new HashMap<>();
        
        for (Map.Entry<String, TaskErrorHandler.TaskErrorStats> entry : allStats.entrySet()) {
            String taskType = entry.getKey();
            TaskErrorHandler.TaskErrorStats stats = entry.getValue();
            
            long errorCount = stats.getTotalErrorCount();
            totalErrors += errorCount;
            taskTypesWithErrors++;
            errorsByTaskType.put(taskType, errorCount);
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalErrors", totalErrors);
        summary.put("taskTypesWithErrors", taskTypesWithErrors);
        summary.put("errorsByTaskType", errorsByTaskType);
        summary.put("recentErrors", errorHandler.getAllRecentErrors(10));
        
        return ResponseEntity.ok(summary);
    }

    /**
     * Clear error statistics (for testing/admin purposes)
     */
    @DeleteMapping("/errors/stats")
    public ResponseEntity<Map<String, String>> clearErrorStats() {
        log.info("DELETE /monitoring/errors/stats");
        
        errorHandler.clearStats();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Error statistics cleared successfully");
        response.put("status", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get health status of error monitoring system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        log.info("GET /monitoring/health");
        
        Map<String, TaskErrorHandler.TaskErrorStats> allStats = errorHandler.getAllErrorStats();
        
        long totalErrors = allStats.values().stream()
                .mapToLong(TaskErrorHandler.TaskErrorStats::getTotalErrorCount)
                .sum();
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("errorMonitoringEnabled", true);
        health.put("totalTaskTypes", allStats.size());
        health.put("totalErrorsTracked", totalErrors);
        
        return ResponseEntity.ok(health);
    }
}
