package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.ExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ExecutionHistory entity.
 * Provides queries for execution history tracking and analytics.
 */
@Repository
public interface ExecutionHistoryRepository extends MongoRepository<ExecutionHistory, String> {

    /**
     * Find execution by Conductor workflow execution ID
     */
    Optional<ExecutionHistory> findByWorkflowExecutionId(String workflowExecutionId);

    /**
     * Find all executions for a specific workflow definition
     */
    Page<ExecutionHistory> findByWorkflowDefinitionId(String workflowDefinitionId, Pageable pageable);

    /**
     * Find all executions for a specific workflow definition ordered by start time
     */
    List<ExecutionHistory> findByWorkflowDefinitionIdOrderByStartTimeDesc(String workflowDefinitionId);

    /**
     * Find all executions by status
     */
    Page<ExecutionHistory> findByStatus(ExecutionHistory.ExecutionStatus status, Pageable pageable);

    /**
     * Find all executions by project ID
     */
    Page<ExecutionHistory> findByProjectId(String projectId, Pageable pageable);

    /**
     * Find all executions by user
     */
    Page<ExecutionHistory> findByExecutedBy(String executedBy, Pageable pageable);

    /**
     * Find executions by project and status
     */
    Page<ExecutionHistory> findByProjectIdAndStatus(
        String projectId, 
        ExecutionHistory.ExecutionStatus status, 
        Pageable pageable
    );

    /**
     * Find executions within date range
     */
    List<ExecutionHistory> findByStartTimeBetween(Instant startDate, Instant endDate);

    /**
     * Find executions by project and date range
     */
    List<ExecutionHistory> findByProjectIdAndStartTimeBetween(
        String projectId, 
        Instant startDate, 
        Instant endDate
    );

    /**
     * Count executions by status
     */
    long countByStatus(ExecutionHistory.ExecutionStatus status);

    /**
     * Count executions by project
     */
    long countByProjectId(String projectId);

    /**
     * Count executions by workflow definition
     */
    long countByWorkflowDefinitionId(String workflowDefinitionId);

    /**
     * Find running executions for a workflow
     */
    List<ExecutionHistory> findByWorkflowDefinitionIdAndStatus(
        String workflowDefinitionId, 
        ExecutionHistory.ExecutionStatus status
    );

    /**
     * Find recent executions (last N)
     */
    List<ExecutionHistory> findTop10ByOrderByStartTimeDesc();

    /**
     * Find recent executions for a project
     */
    List<ExecutionHistory> findTop10ByProjectIdOrderByStartTimeDesc(String projectId);

    /**
     * Custom query: Find executions with failures
     */
    @Query("{ 'status': { $in: ['FAILED', 'FAILED_WITH_TERMINAL_ERROR', 'TERMINATED'] } }")
    Page<ExecutionHistory> findFailedExecutions(Pageable pageable);

    /**
     * Custom query: Find long-running executions (duration > threshold)
     */
    @Query("{ 'durationMs': { $gt: ?0 }, 'status': 'COMPLETED' }")
    List<ExecutionHistory> findLongRunningExecutions(long durationThresholdMs);
}
