package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WorkflowEvent entities
 */
@Repository
public interface WorkflowEventRepository extends MongoRepository<WorkflowEvent, String> {
    
    /**
     * Find all unprocessed events
     */
    List<WorkflowEvent> findByProcessed(Boolean processed);
    
    /**
     * Find all events by type
     */
    List<WorkflowEvent> findByEventType(String eventType);
    
    /**
     * Find all unprocessed events by type
     */
    List<WorkflowEvent> findByEventTypeAndProcessed(String eventType, Boolean processed);
    
    /**
     * Find all events for a project
     */
    List<WorkflowEvent> findByProjectId(String projectId);
    
    /**
     * Find all events by status
     */
    List<WorkflowEvent> findByStatus(String status);
    
    /**
     * Find all events within a time range
     */
    List<WorkflowEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find all events by source
     */
    List<WorkflowEvent> findByEventSource(String eventSource);
    
    /**
     * Find all unprocessed events for a project
     */
    List<WorkflowEvent> findByProjectIdAndProcessed(String projectId, Boolean processed);
    
    /**
     * Find events by user
     */
    List<WorkflowEvent> findByUserId(String userId);
    
    /**
     * Delete old processed events (for cleanup)
     */
    void deleteByProcessedAndTimestampBefore(Boolean processed, LocalDateTime timestamp);
}
