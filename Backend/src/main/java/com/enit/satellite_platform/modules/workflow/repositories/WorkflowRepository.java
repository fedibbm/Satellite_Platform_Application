package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.Workflow;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
    List<Workflow> findByCreatedBy(String createdBy);
    List<Workflow> findByProjectId(ObjectId projectId);
    List<Workflow> findByStatus(WorkflowStatus status);
    List<Workflow> findByIsTemplate(Boolean isTemplate);
    Optional<Workflow> findByIdAndCreatedBy(String id, String createdBy);
}
