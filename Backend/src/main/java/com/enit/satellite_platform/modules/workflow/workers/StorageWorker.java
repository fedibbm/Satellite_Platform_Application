package com.enit.satellite_platform.modules.workflow.workers;

import com.enit.satellite_platform.modules.workflow.services.CompensationHandler;
import com.netflix.conductor.common.metadata.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Worker for save_results tasks.
 * Stores processing results to database and file system.
 */
@Slf4j
@Component
public class StorageWorker extends BaseTaskWorker {

    @Value("${storage.results.path:./upload-dir/results}")
    private String resultsPath;

    @Override
    public String getTaskDefName() {
        return "save_results";
    }

    @Override
    protected Map<String, Object> executeTask(Task task) throws Exception {
        log.info("StorageWorker: Saving workflow results");
        
        // Get required parameters
        String workflowId = (String) getRequiredInput(task, "workflowId");
        String projectId = (String) getRequiredInput(task, "projectId");
        Object results = getRequiredInput(task, "results");
        
        // Optional parameters
        String userId = (String) getOptionalInput(task, "userId", "system");
        String format = (String) getOptionalInput(task, "format", "json");
        
        log.info("StorageWorker: WorkflowId={}, Project={}, User={}", 
            workflowId, projectId, userId);
        
        try {
            // Create storage directory if not exists
            Path storagePath = Paths.get(resultsPath, projectId, workflowId);
            Files.createDirectories(storagePath);
            
            // Generate unique result ID
            String resultId = UUID.randomUUID().toString();
            String timestamp = Instant.now().toString();
            
            // Save results to file
            String fileName = String.format("result_%s_%s.%s", 
                timestamp.replace(":", "-"), resultId, format);
            Path filePath = storagePath.resolve(fileName);
            
            // Write results to file
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                if (results instanceof String) {
                    writer.write((String) results);
                } else {
                    // Convert to JSON-like string representation
                    writer.write(results.toString());
                }
            }
            
            log.info("StorageWorker: Results saved to {}", filePath.toAbsolutePath());
            
            // Create output with storage metadata
            Map<String, Object> output = createOutput();
            output.put("resultId", resultId);
            output.put("filePath", filePath.toAbsolutePath().toString());
            output.put("fileName", fileName);
            output.put("workflowId", workflowId);
            output.put("projectId", projectId);
            output.put("userId", userId);
            output.put("savedAt", timestamp);
            output.put("format", format);
            output.put("status", "saved");
            
            log.info("StorageWorker: Results saved successfully with ID: {}", resultId);
            return output;
            
        } catch (Exception e) {
            log.error("StorageWorker: Failed to save results: {}", e.getMessage());
            throw new RuntimeException("Failed to save results: " + e.getMessage(), e);
        }
    }

    @Override
    protected void registerCompensationActions(Task task) {
        // Register compensation to clean up files if workflow fails later
        if (compensationHandler != null) {
            String workflowId = (String) task.getInputData().get("workflowId");
            String projectId = (String) task.getInputData().get("projectId");
            
            if (workflowId != null && projectId != null) {
                Path storagePath = Paths.get(resultsPath, projectId, workflowId);
                compensationHandler.registerCompensation(
                    task.getWorkflowInstanceId(),
                    CompensationHandler.deleteDirectory(storagePath)
                );
                log.debug("Registered compensation to delete directory: {}", storagePath);
            }
        }
    }

    @Override
    protected void validateInput(Task task) throws IllegalArgumentException {
        super.validateInput(task);
        
        String workflowId = (String) task.getInputData().get("workflowId");
        if (workflowId == null || workflowId.trim().isEmpty()) {
            throw new IllegalArgumentException("workflowId parameter is required");
        }
        
        String projectId = (String) task.getInputData().get("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("projectId parameter is required");
        }
        
        Object results = task.getInputData().get("results");
        if (results == null) {
            throw new IllegalArgumentException("results parameter is required");
        }
    }

    @Override
    public int getThreadCount() {
        return 2; // Allow parallel storage operations
    }
}
