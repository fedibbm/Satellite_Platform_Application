package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowDefinition entity
 */
@Repository
public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, String> {
    
    /**
     * Find workflows by project ID
     */
    List<WorkflowDefinition> findByProjectId(String projectId);
    
    /**
     * Find workflows by status
     */
    List<WorkflowDefinition> findByStatus(String status);
    
    /**
     * Find workflows by created by user
     */
    List<WorkflowDefinition> findByCreatedBy(String userId);
    
    /**
     * Find workflow by name and project
     */
    Optional<WorkflowDefinition> findByNameAndProjectId(String name, String projectId);
    
    /**
     * Find published workflows for a project
     */
    List<WorkflowDefinition> findByProjectIdAndStatus(String projectId, String status);
}
