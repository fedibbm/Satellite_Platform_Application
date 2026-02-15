package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.ExecutionHistory;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import io.orkes.conductor.client.http.OrkesWorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for executing workflows in Conductor.
 * Handles workflow start, status checks, and result retrieval.
 * Integrated with ExecutionHistoryService for tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final OrkesWorkflowClient workflowClient;
    private final ExecutionHistoryService executionHistoryService;

    /**
     * Start a workflow execution with input parameters.
     *
     * @param workflowName Name of the workflow to start
     * @param version Workflow version (use 1 for latest)
     * @param input Input parameters for the workflow
     * @param workflowDefinitionId MongoDB workflow definition ID
     * @param projectId Project ID
     * @param executedBy User who started the execution
     * @return Workflow execution ID
     */
    public String startWorkflow(
            String workflowName, 
            Integer version, 
            Map<String, Object> input,
            String workflowDefinitionId,
            String projectId,
            String executedBy) {
        try {
            log.info("Starting workflow: {} (version: {}) for project: {}", workflowName, version, projectId);
            
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName(workflowName);
            request.setVersion(version);
            request.setInput(input != null ? input : new HashMap<>());
            
            String workflowId = workflowClient.startWorkflow(request);
            
            log.info("Workflow started successfully. Execution ID: {}", workflowId);
            
            // Create execution history record
            executionHistoryService.createExecutionHistory(
                workflowId, 
                workflowDefinitionId, 
                workflowName, 
                projectId, 
                executedBy, 
                input
            );
            
            return workflowId;
            
        } catch (Exception e) {
            log.error("Failed to start workflow {}: {}", workflowName, e.getMessage(), e);
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Get workflow execution status.
     *
     * @param workflowId Workflow execution ID
     * @return Workflow status and details
     */
    public Map<String, Object> getWorkflowStatus(String workflowId) {
        try {
            log.info("Fetching workflow status for: {}", workflowId);
            
            var workflow = workflowClient.getWorkflow(workflowId, false);
            
            // Update execution history
            executionHistoryService.updateFromWorkflow(workflow);
            
            Map<String, Object> status = new HashMap<>();
            status.put("workflowId", workflow.getWorkflowId());
            status.put("status", workflow.getStatus().toString());
            status.put("workflowName", workflow.getWorkflowName());
            // version not available in Orkes Workflow class
            status.put("createTime", workflow.getCreateTime());
            status.put("updateTime", workflow.getUpdateTime());
            status.put("input", workflow.getInput());
            status.put("output", workflow.getOutput());
            status.put("failedReferenceTaskNames", workflow.getFailedReferenceTaskNames());
            
            return status;
            
        } catch (Exception e) {
            log.error("Failed to get workflow status for {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to get workflow status: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed workflow execution with task details.
     *
     * @param workflowId Workflow execution ID
     * @return Complete workflow execution details
     */
    public Map<String, Object> getWorkflowDetails(String workflowId) {
        try {
            log.info("Fetching workflow details for: {}", workflowId);
            
            var workflow = workflowClient.getWorkflow(workflowId, true);
            
            // Update execution history
            executionHistoryService.updateFromWorkflow(workflow);
            
            Map<String, Object> details = new HashMap<>();
            details.put("workflowId", workflow.getWorkflowId());
            details.put("status", workflow.getStatus().toString());
            details.put("workflowName", workflow.getWorkflowName());
            // version not available in Orkes Workflow class
            details.put("createTime", workflow.getCreateTime());
            details.put("updateTime", workflow.getUpdateTime());
            details.put("input", workflow.getInput());
            details.put("output", workflow.getOutput());
            details.put("tasks", workflow.getTasks());
            details.put("failedReferenceTaskNames", workflow.getFailedReferenceTaskNames());
            details.put("reasonForIncompletion", workflow.getReasonForIncompletion());
            
            return details;
            
        } catch (Exception e) {
            log.error("Failed to get workflow details for {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to get workflow details: " + e.getMessage(), e);
        }
    }

    /**
     * Terminate a running workflow.
     *
     * @param workflowId Workflow execution ID
     * @param reason Reason for termination
     */
    public void terminateWorkflow(String workflowId, String reason) {
        try {
            log.info("Terminating workflow {}: {}", workflowId, reason);
            workflowClient.terminateWorkflow(workflowId, reason);
            log.info("Workflow terminated successfully");
        } catch (Exception e) {
            log.error("Failed to terminate workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to terminate workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Pause a running workflow.
     *
     * @param workflowId Workflow execution ID
     */
    public void pauseWorkflow(String workflowId) {
        try {
            log.info("Pausing workflow: {}", workflowId);
            workflowClient.pauseWorkflow(workflowId);
            log.info("Workflow paused successfully");
        } catch (Exception e) {
            log.error("Failed to pause workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to pause workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Resume a paused workflow.
     *
     * @param workflowId Workflow execution ID
     */
    public void resumeWorkflow(String workflowId) {
        try {
            log.info("Resuming workflow: {}", workflowId);
            workflowClient.resumeWorkflow(workflowId);
            log.info("Workflow resumed successfully");
        } catch (Exception e) {
            log.error("Failed to resume workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to resume workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Restart a completed workflow.
     *
     * @param workflowId Workflow execution ID
     */
    public void restartWorkflow(String workflowId) {
        try {
            log.info("Restarting workflow: {}", workflowId);
            // Orkes client doesn't have restart/rerun, use retry instead
            workflowClient.retryLastFailedTask(workflowId);
            log.info("Workflow restarted successfully");
        } catch (Exception e) {
            log.error("Failed to restart workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to restart workflow: " + e.getMessage(), e);
        }
    }
}
