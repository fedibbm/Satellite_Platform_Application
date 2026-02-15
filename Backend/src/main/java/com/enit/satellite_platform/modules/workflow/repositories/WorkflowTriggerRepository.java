package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowTrigger entities
 */
@Repository
public interface WorkflowTriggerRepository extends MongoRepository<WorkflowTrigger, String> {
    
    /**
     * Find all triggers for a specific project
     */
    List<WorkflowTrigger> findByProjectId(String projectId);
    
    /**
     * Find all triggers for a specific workflow definition
     */
    List<WorkflowTrigger> findByWorkflowDefinitionId(String workflowDefinitionId);
    
    /**
     * Find all enabled triggers
     */
    List<WorkflowTrigger> findByEnabled(Boolean enabled);
    
    /**
     * Find all triggers by type
     */
    List<WorkflowTrigger> findByType(WorkflowTrigger.TriggerType type);
    
    /**
     * Find all enabled triggers by type
     */
    List<WorkflowTrigger> findByEnabledAndType(Boolean enabled, WorkflowTrigger.TriggerType type);
    
    /**
     * Find all enabled triggers for a project
     */
    List<WorkflowTrigger> findByProjectIdAndEnabled(String projectId, Boolean enabled);
    
    /**
     * Find trigger by name in a project
     */
    Optional<WorkflowTrigger> findByProjectIdAndName(String projectId, String name);
    
    /**
     * Find all triggers created by a user
     */
    List<WorkflowTrigger> findByCreatedBy(String userId);
}
