package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {
    List<WorkflowExecution> findByWorkflowId(String workflowId);
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(String workflowId);
    List<WorkflowExecution> findByStatus(String status);
    List<WorkflowExecution> findByTriggeredBy(String triggeredBy);
}
