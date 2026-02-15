package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.ExecutionHistory;
import com.enit.satellite_platform.modules.workflow.repositories.ExecutionHistoryRepository;
import com.netflix.conductor.common.run.Workflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing workflow execution history.
 * Tracks all workflow executions in MongoDB for analytics and monitoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionHistoryService {

    private final ExecutionHistoryRepository executionHistoryRepository;

    /**
     * Create execution history record when workflow starts
     */
    public ExecutionHistory createExecutionHistory(
            String workflowExecutionId,
            String workflowDefinitionId,
            String workflowName,
            String projectId,
            String executedBy,
            Map<String, Object> inputParameters) {
        
        log.info("Creating execution history for workflow: {}", workflowExecutionId);
        
        ExecutionHistory history = ExecutionHistory.builder()
                .workflowExecutionId(workflowExecutionId)
                .workflowDefinitionId(workflowDefinitionId)
                .workflowName(workflowName)
                .projectId(projectId)
                .executedBy(executedBy)
                .status(ExecutionHistory.ExecutionStatus.RUNNING)
                .startTime(Instant.now())
                .inputParameters(inputParameters)
                .lastUpdated(Instant.now())
                .restartable(true)
                .metadata(new HashMap<>())
                .build();
        
        return executionHistoryRepository.save(history);
    }

    /**
     * Update execution history from Conductor workflow object
     */
    public ExecutionHistory updateFromWorkflow(Workflow workflow) {
        Optional<ExecutionHistory> existingOpt = 
            executionHistoryRepository.findByWorkflowExecutionId(workflow.getWorkflowId());
        
        ExecutionHistory history;
        if (existingOpt.isPresent()) {
            history = existingOpt.get();
        } else {
            // Create new if doesn't exist
            history = ExecutionHistory.builder()
                    .workflowExecutionId(workflow.getWorkflowId())
                    .workflowName(workflow.getWorkflowName())
                    .startTime(Instant.ofEpochMilli(workflow.getCreateTime()))
                    .build();
        }
        
        // Update status
        history.setStatus(mapConductorStatus(workflow.getStatus()));
        history.setLastUpdated(Instant.now());
        
        // Update timing
        if (workflow.getEndTime() > 0) {
            history.setEndTime(Instant.ofEpochMilli(workflow.getEndTime()));
            history.setDurationMs(workflow.getEndTime() - workflow.getCreateTime());
        }
        
        // Update task counts
        if (workflow.getTasks() != null) {
            history.setTotalTasks(workflow.getTasks().size());
            long completed = workflow.getTasks().stream()
                .filter(t -> t.getStatus().isTerminal() && t.getStatus().isSuccessful())
                .count();
            long failed = workflow.getTasks().stream()
                .filter(t -> t.getStatus().isTerminal() && !t.getStatus().isSuccessful())
                .count();
            history.setCompletedTasks((int) completed);
            history.setFailedTasksCount((int) failed);
        }
        
        // Update failure info
        if (workflow.getReasonForIncompletion() != null) {
            history.setFailureReason(workflow.getReasonForIncompletion());
        }
        if (workflow.getFailedReferenceTaskNames() != null && !workflow.getFailedReferenceTaskNames().isEmpty()) {
            history.setFailedTasks(new java.util.ArrayList<>(workflow.getFailedReferenceTaskNames()));
        }
        
        // Update input/output
        if (workflow.getInput() != null) {
            history.setInputParameters(workflow.getInput());
        }
        if (workflow.getOutput() != null) {
            history.setOutputData(workflow.getOutput());
        }
        
        return executionHistoryRepository.save(history);
    }

    /**
     * Update execution status
     */
    public ExecutionHistory updateStatus(String workflowExecutionId, ExecutionHistory.ExecutionStatus status) {
        ExecutionHistory history = executionHistoryRepository
            .findByWorkflowExecutionId(workflowExecutionId)
            .orElseThrow(() -> new RuntimeException("Execution history not found: " + workflowExecutionId));
        
        history.setStatus(status);
        history.setLastUpdated(Instant.now());
        
        // Set end time if terminal status
        if (isTerminalStatus(status) && history.getEndTime() == null) {
            history.setEndTime(Instant.now());
            if (history.getStartTime() != null) {
                history.setDurationMs(
                    history.getEndTime().toEpochMilli() - history.getStartTime().toEpochMilli()
                );
            }
        }
        
        return executionHistoryRepository.save(history);
    }

    /**
     * Get execution history by workflow execution ID
     */
    public Optional<ExecutionHistory> getByWorkflowExecutionId(String workflowExecutionId) {
        return executionHistoryRepository.findByWorkflowExecutionId(workflowExecutionId);
    }

    /**
     * Get execution history for a workflow definition
     */
    public Page<ExecutionHistory> getByWorkflowDefinitionId(String workflowDefinitionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return executionHistoryRepository.findByWorkflowDefinitionId(workflowDefinitionId, pageable);
    }

    /**
     * Get execution history by project
     */
    public Page<ExecutionHistory> getByProjectId(String projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return executionHistoryRepository.findByProjectId(projectId, pageable);
    }

    /**
     * Get execution history by status
     */
    public Page<ExecutionHistory> getByStatus(ExecutionHistory.ExecutionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        return executionHistoryRepository.findByStatus(status, pageable);
    }

    /**
     * Get recent executions
     */
    public List<ExecutionHistory> getRecentExecutions(int limit) {
        return executionHistoryRepository.findTop10ByOrderByStartTimeDesc();
    }

    /**
     * Get execution statistics
     */
    public Map<String, Object> getExecutionStatistics(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (projectId != null) {
            stats.put("totalExecutions", executionHistoryRepository.countByProjectId(projectId));
            stats.put("runningExecutions", 
                executionHistoryRepository.findByProjectIdAndStatus(
                    projectId, ExecutionHistory.ExecutionStatus.RUNNING, Pageable.unpaged()).getContent().size());
            
            List<ExecutionHistory> recentExecutions = 
                executionHistoryRepository.findTop10ByProjectIdOrderByStartTimeDesc(projectId);
            stats.put("recentExecutions", recentExecutions);
        } else {
            stats.put("totalExecutions", executionHistoryRepository.count());
            stats.put("runningExecutions", 
                executionHistoryRepository.countByStatus(ExecutionHistory.ExecutionStatus.RUNNING));
            stats.put("completedExecutions", 
                executionHistoryRepository.countByStatus(ExecutionHistory.ExecutionStatus.COMPLETED));
            stats.put("failedExecutions", 
                executionHistoryRepository.countByStatus(ExecutionHistory.ExecutionStatus.FAILED));
        }
        
        return stats;
    }

    /**
     * Map Conductor workflow status to ExecutionHistory status
     */
    private ExecutionHistory.ExecutionStatus mapConductorStatus(Workflow.WorkflowStatus conductorStatus) {
        return switch (conductorStatus) {
            case RUNNING -> ExecutionHistory.ExecutionStatus.RUNNING;
            case COMPLETED -> ExecutionHistory.ExecutionStatus.COMPLETED;
            case FAILED -> ExecutionHistory.ExecutionStatus.FAILED;
            case TIMED_OUT -> ExecutionHistory.ExecutionStatus.TIMED_OUT;
            case TERMINATED -> ExecutionHistory.ExecutionStatus.TERMINATED;
            case PAUSED -> ExecutionHistory.ExecutionStatus.PAUSED;
            default -> ExecutionHistory.ExecutionStatus.FAILED;
        };
    }

    /**
     * Check if status is terminal (execution ended)
     */
    private boolean isTerminalStatus(ExecutionHistory.ExecutionStatus status) {
        return status == ExecutionHistory.ExecutionStatus.COMPLETED ||
               status == ExecutionHistory.ExecutionStatus.FAILED ||
               status == ExecutionHistory.ExecutionStatus.FAILED_WITH_TERMINAL_ERROR ||
               status == ExecutionHistory.ExecutionStatus.TERMINATED ||
               status == ExecutionHistory.ExecutionStatus.TIMED_OUT;
    }
}
