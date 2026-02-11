package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import com.enit.satellite_platform.modules.workflow.entities.ExecutionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {
    List<WorkflowExecution> findByWorkflowId(String workflowId);
    List<WorkflowExecution> findByTriggeredBy(String triggeredBy);
    List<WorkflowExecution> findByStatus(ExecutionStatus status);
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(String workflowId);
}
