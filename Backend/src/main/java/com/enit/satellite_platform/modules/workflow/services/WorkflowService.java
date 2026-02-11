package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.entities.*;
import com.enit.satellite_platform.modules.workflow.mapper.WorkflowMapper;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowExecutionRepository;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private WorkflowMapper workflowMapper;

    public List<WorkflowDTO> getAllWorkflows(String userEmail) {
        logger.info("Fetching all workflows for user: {}", userEmail);
        List<Workflow> workflows = workflowRepository.findByCreatedBy(userEmail);
        return workflowMapper.toDTOList(workflows);
    }

    public List<WorkflowDTO> getWorkflowsByProject(String projectId, String userEmail) {
        logger.info("Fetching workflows for project: {} and user: {}", projectId, userEmail);
        ObjectId objectId = new ObjectId(projectId);
        List<Workflow> workflows = workflowRepository.findByProjectId(objectId);
        // Filter by user
        workflows = workflows.stream()
                .filter(w -> w.getCreatedBy().equals(userEmail))
                .toList();
        return workflowMapper.toDTOList(workflows);
    }

    public List<WorkflowDTO> getWorkflowTemplates() {
        logger.info("Fetching workflow templates");
        List<Workflow> templates = workflowRepository.findByIsTemplate(true);
        return workflowMapper.toDTOList(templates);
    }

    public WorkflowDTO getWorkflowById(String id, String userEmail) {
        logger.info("Fetching workflow with id: {} for user: {}", id, userEmail);
        Optional<Workflow> workflow = workflowRepository.findByIdAndCreatedBy(id, userEmail);
        
        if (workflow.isEmpty()) {
            throw new RuntimeException("Workflow not found or access denied");
        }

        // Get executions for this workflow
        List<WorkflowExecution> executions = executionRepository.findByWorkflowIdOrderByStartedAtDesc(id);
        
        return workflowMapper.toDTOWithExecutions(workflow.get(), executions);
    }

    public WorkflowDTO createWorkflow(CreateWorkflowRequest request, String userEmail) {
        logger.info("Creating new workflow: {} for user: {}", request.getName(), userEmail);

        Workflow workflow = new Workflow();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setCreatedBy(userEmail);
        workflow.setIsTemplate(request.getIsTemplate() != null ? request.getIsTemplate() : false);
        
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            workflow.setProjectId(new ObjectId(request.getProjectId()));
        }

        // Create initial version
        WorkflowVersion initialVersion = new WorkflowVersion();
        initialVersion.setVersion("v1.0");
        initialVersion.setCreatedAt(LocalDateTime.now());
        initialVersion.setCreatedBy(userEmail);
        initialVersion.setNodes(request.getNodes() != null ? request.getNodes() : new ArrayList<>());
        initialVersion.setEdges(request.getEdges() != null ? request.getEdges() : new ArrayList<>());
        initialVersion.setChangelog("Initial version");

        workflow.setCurrentVersion("v1.0");
        workflow.getVersions().add(initialVersion);

        Workflow savedWorkflow = workflowRepository.save(workflow);
        logger.info("Workflow created with id: {}", savedWorkflow.getId());

        return workflowMapper.toDTO(savedWorkflow);
    }

    public WorkflowDTO updateWorkflow(String id, UpdateWorkflowRequest request, String userEmail) {
        logger.info("Updating workflow: {} for user: {}", id, userEmail);

        Workflow workflow = workflowRepository.findByIdAndCreatedBy(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Workflow not found or access denied"));

        // Update basic fields
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }

        workflow.setUpdatedAt(LocalDateTime.now());

        // If nodes or edges are updated, create a new version
        if ((request.getNodes() != null && !request.getNodes().isEmpty()) || 
            (request.getEdges() != null && !request.getEdges().isEmpty())) {
            
            String newVersionNumber = generateNextVersion(workflow.getCurrentVersion());
            
            WorkflowVersion newVersion = new WorkflowVersion();
            newVersion.setVersion(newVersionNumber);
            newVersion.setCreatedAt(LocalDateTime.now());
            newVersion.setCreatedBy(userEmail);
            newVersion.setNodes(request.getNodes() != null ? request.getNodes() : new ArrayList<>());
            newVersion.setEdges(request.getEdges() != null ? request.getEdges() : new ArrayList<>());
            newVersion.setChangelog(request.getChangelog() != null ? request.getChangelog() : "Updated workflow");

            workflow.getVersions().add(newVersion);
            workflow.setCurrentVersion(newVersionNumber);
        }

        Workflow updatedWorkflow = workflowRepository.save(workflow);
        logger.info("Workflow updated: {}", id);

        return workflowMapper.toDTO(updatedWorkflow);
    }

    public void deleteWorkflow(String id, String userEmail) {
        logger.info("Deleting workflow: {} for user: {}", id, userEmail);

        Workflow workflow = workflowRepository.findByIdAndCreatedBy(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Workflow not found or access denied"));

        // Delete associated executions
        List<WorkflowExecution> executions = executionRepository.findByWorkflowId(id);
        executionRepository.deleteAll(executions);

        workflowRepository.delete(workflow);
        logger.info("Workflow deleted: {}", id);
    }

    private String generateNextVersion(String currentVersion) {
        // Simple version increment: v1.0 -> v1.1
        String numericPart = currentVersion.substring(1); // Remove 'v'
        String[] parts = numericPart.split("\\.");
        int minor = Integer.parseInt(parts[1]) + 1;
        return "v" + parts[0] + "." + minor;
    }
}
