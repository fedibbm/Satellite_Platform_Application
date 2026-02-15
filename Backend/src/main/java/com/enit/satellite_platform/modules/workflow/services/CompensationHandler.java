package com.enit.satellite_platform.modules.workflow.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling compensation (rollback) operations when tasks fail
 * Ensures proper cleanup and resource management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationHandler {

    // Track compensation actions for workflows
    private final Map<String, List<CompensationAction>> compensationActions = new ConcurrentHashMap<>();

    /**
     * Register a compensation action for a workflow
     * @param workflowId the workflow execution ID
     * @param action the compensation action to register
     */
    public void registerCompensation(String workflowId, CompensationAction action) {
        log.debug("Registering compensation action for workflow {}: {}", workflowId, action.getDescription());
        
        compensationActions.computeIfAbsent(workflowId, k -> new ArrayList<>())
                          .add(action);
    }

    /**
     * Execute all compensation actions for a failed workflow
     * @param workflowId the workflow execution ID
     * @return CompensationResult with success status and details
     */
    public CompensationResult compensate(String workflowId, String reason) {
        log.info("Starting compensation for workflow {}, reason: {}", workflowId, reason);

        List<CompensationAction> actions = compensationActions.get(workflowId);
        if (actions == null || actions.isEmpty()) {
            log.info("No compensation actions registered for workflow {}", workflowId);
            return CompensationResult.success(workflowId, 0);
        }

        int totalActions = actions.size();
        int successfulActions = 0;
        int failedActions = 0;
        List<String> errors = new ArrayList<>();

        // Execute compensation actions in reverse order (LIFO - Last In First Out)
        for (int i = actions.size() - 1; i >= 0; i--) {
            CompensationAction action = actions.get(i);
            try {
                log.debug("Executing compensation action: {}", action.getDescription());
                action.execute();
                successfulActions++;
                log.debug("Compensation action completed successfully: {}", action.getDescription());
            } catch (Exception e) {
                failedActions++;
                String errorMsg = String.format("Failed to execute compensation action '%s': %s", 
                                               action.getDescription(), e.getMessage());
                errors.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        // Clear compensation actions for this workflow
        compensationActions.remove(workflowId);

        CompensationResult result = new CompensationResult();
        result.setWorkflowId(workflowId);
        result.setSuccess(failedActions == 0);
        result.setTotalActions(totalActions);
        result.setSuccessfulActions(successfulActions);
        result.setFailedActions(failedActions);
        result.setErrors(errors);
        result.setCompensationTime(Instant.now());

        log.info("Compensation completed for workflow {}: {}/{} actions successful", 
                 workflowId, successfulActions, totalActions);

        return result;
    }

    /**
     * Clear all compensation actions for a workflow (used when workflow completes successfully)
     * @param workflowId the workflow execution ID
     */
    public void clearCompensation(String workflowId) {
        List<CompensationAction> actions = compensationActions.remove(workflowId);
        if (actions != null) {
            log.debug("Cleared {} compensation actions for successful workflow {}", actions.size(), workflowId);
        }
    }

    /**
     * Get registered compensation actions for a workflow
     */
    public List<CompensationAction> getCompensationActions(String workflowId) {
        return new ArrayList<>(compensationActions.getOrDefault(workflowId, new ArrayList<>()));
    }

    // ============== Pre-defined Compensation Actions ==============

    /**
     * Create compensation action to delete a file
     */
    public static CompensationAction deleteFile(Path filePath) {
        return new CompensationAction() {
            @Override
            public void execute() throws Exception {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted file during compensation: {}", filePath);
                }
            }

            @Override
            public String getDescription() {
                return "Delete file: " + filePath;
            }
        };
    }

    /**
     * Create compensation action to delete a directory recursively
     */
    public static CompensationAction deleteDirectory(Path dirPath) {
        return new CompensationAction() {
            @Override
            public void execute() throws Exception {
                if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                    Files.walk(dirPath)
                         .sorted((p1, p2) -> -p1.compareTo(p2)) // Reverse order to delete files before directories
                         .forEach(path -> {
                             try {
                                 Files.delete(path);
                             } catch (IOException e) {
                                 log.warn("Failed to delete {} during compensation: {}", path, e.getMessage());
                             }
                         });
                    log.info("Deleted directory during compensation: {}", dirPath);
                }
            }

            @Override
            public String getDescription() {
                return "Delete directory: " + dirPath;
            }
        };
    }

    /**
     * Create compensation action for custom cleanup logic
     */
    public static CompensationAction custom(String description, CompensationLogic logic) {
        return new CompensationAction() {
            @Override
            public void execute() throws Exception {
                logic.compensate();
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    /**
     * Create compensation action to clean up temporary GEE data
     */
    public static CompensationAction cleanupGEEData(String imageId) {
        return custom("Cleanup GEE data for image: " + imageId, () -> {
            // Clean up any cached GEE data
            Path cachePath = Paths.get("./gee_cache/" + imageId);
            if (Files.exists(cachePath)) {
                Files.delete(cachePath);
                log.info("Cleaned up GEE cache for image: {}", imageId);
            }
        });
    }

    /**
     * Create compensation action to clean up NDVI processing results
     */
    public static CompensationAction cleanupNDVIResults(String workflowId, String imageId) {
        return custom("Cleanup NDVI results for workflow: " + workflowId, () -> {
            Path resultsPath = Paths.get("./upload-dir/results/" + workflowId);
            if (Files.exists(resultsPath)) {
                deleteDirectory(resultsPath).execute();
                log.info("Cleaned up NDVI results for workflow: {}", workflowId);
            }
        });
    }

    // ============== Interfaces and Classes ==============

    /**
     * Functional interface for compensation actions
     */
    @FunctionalInterface
    public interface CompensationAction {
        /**
         * Execute the compensation action
         * @throws Exception if compensation fails
         */
        void execute() throws Exception;

        /**
         * Get description of the compensation action
         */
        default String getDescription() {
            return "Compensation action";
        }
    }

    /**
     * Functional interface for custom compensation logic
     */
    @FunctionalInterface
    public interface CompensationLogic {
        void compensate() throws Exception;
    }

    /**
     * Result of compensation execution
     */
    @Data
    public static class CompensationResult {
        private String workflowId;
        private boolean success;
        private int totalActions;
        private int successfulActions;
        private int failedActions;
        private List<String> errors;
        private Instant compensationTime;

        public static CompensationResult success(String workflowId, int actionsExecuted) {
            CompensationResult result = new CompensationResult();
            result.setWorkflowId(workflowId);
            result.setSuccess(true);
            result.setTotalActions(actionsExecuted);
            result.setSuccessfulActions(actionsExecuted);
            result.setFailedActions(0);
            result.setErrors(new ArrayList<>());
            result.setCompensationTime(Instant.now());
            return result;
        }
    }
}
