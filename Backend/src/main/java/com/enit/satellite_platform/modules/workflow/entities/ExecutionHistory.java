package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a workflow execution record.
 * Stores execution history in MongoDB for tracking and analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "execution_history")
public class ExecutionHistory {

    /**
     * Unique identifier for this execution record
     */
    @Id
    private String id;

    /**
     * Conductor workflow execution ID
     */
    private String workflowExecutionId;

    /**
     * Reference to WorkflowDefinition ID in MongoDB
     */
    private String workflowDefinitionId;

    /**
     * Workflow name in Conductor
     */
    private String workflowName;

    /**
     * Project ID that owns this workflow
     */
    private String projectId;

    /**
     * User who started the execution
     */
    private String executedBy;

    /**
     * Execution status (RUNNING, COMPLETED, FAILED, TERMINATED, PAUSED, etc.)
     */
    private ExecutionStatus status;

    /**
     * When the execution started
     */
    private Instant startTime;

    /**
     * When the execution ended (completed/failed/terminated)
     */
    private Instant endTime;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Input parameters passed to the workflow
     */
    private Map<String, Object> inputParameters;

    /**
     * Output data from the workflow execution
     */
    private Map<String, Object> outputData;

    /**
     * List of failed task reference names
     */
    private List<String> failedTasks;

    /**
     * Reason for failure or termination
     */
    private String failureReason;

    /**
     * Number of tasks in the workflow
     */
    private Integer totalTasks;

    /**
     * Number of completed tasks
     */
    private Integer completedTasks;

    /**
     * Number of failed tasks
     */
    private Integer failedTasksCount;

    /**
     * Execution metadata (tags, labels, etc.)
     */
    private Map<String, String> metadata;

    /**
     * Last update timestamp
     */
    private Instant lastUpdated;

    /**
     * Version of the workflow definition used
     */
    private String version;

    /**
     * Whether the execution can be restarted
     */
    private Boolean restartable;

    /**
     * Execution status enum
     */
    public enum ExecutionStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        FAILED_WITH_TERMINAL_ERROR,
        TERMINATED,
        TIMED_OUT,
        PAUSED
    }
}
