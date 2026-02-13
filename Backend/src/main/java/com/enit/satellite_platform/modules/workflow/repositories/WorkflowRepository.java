package com.enit.satellite_platform.modules.workflow.repositories;

import com.enit.satellite_platform.modules.workflow.entities.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
    List<Workflow> findByProjectId(String projectId);
    List<Workflow> findByCreatedBy(String createdBy);
    List<Workflow> findByStatus(String status);
    List<Workflow> findByIsTemplate(boolean isTemplate);
    List<Workflow> findByTagsContaining(String tag);
}
