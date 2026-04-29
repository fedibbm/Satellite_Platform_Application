package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.entities.*;
import com.enit.satellite_platform.modules.workflow.mapper.WorkflowMapper;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowExecutionRepository;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.enit.satellite_platform.modules.project_management.entities.PermissionLevel;
import com.enit.satellite_platform.modules.project_management.repositories.ProjectRepository;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.User;
import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
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
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void checkProjectAccess(Workflow workflow, User user, PermissionLevel requiredLevel) {
        if (workflow.getProjectId() != null) {
            Project project = projectRepository.findById(workflow.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
            if (!project.hasAccess(user, requiredLevel)) {
                throw new RuntimeException("Access denied to workflow's project");
            }
        } else if (!workflow.getCreatedBy().equals(user.getEmail())) {
            throw new RuntimeException("Access denied to workflow");
        }
    }

    public List<WorkflowDTO> getAllWorkflows(String userEmail) {
        logger.info("Fetching all workflows for user: {}", userEmail);
        List<Workflow> workflows = workflowRepository.findByCreatedBy(userEmail);
        return workflowMapper.toDTOList(workflows);
    }

    public List<WorkflowDTO> getWorkflowsByProject(String projectId, String userEmail) {
        logger.info("Fetching workflows for project: {} and user: {}", projectId, userEmail);
        User user = getUser(userEmail);
        ObjectId pId = new ObjectId(projectId);
        Project project = projectRepository.findById(pId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
            
        if (!project.hasAccess(user, PermissionLevel.READ)) {
            throw new RuntimeException("Access denied to project");
        }

        List<Workflow> workflows = workflowRepository.findByProjectId(pId);
        return workflowMapper.toDTOList(workflows);
    }

    public List<WorkflowDTO> getWorkflowTemplates() {
        logger.info("Fetching workflow templates");
        List<Workflow> templates = workflowRepository.findByIsTemplate(true);
        return workflowMapper.toDTOList(templates);
    }

    public WorkflowDTO getWorkflowById(String id, String userEmail) {
        logger.info("Fetching workflow with id: {} for user: {}", id, userEmail);
        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
        
        User user = getUser(userEmail);
        checkProjectAccess(workflow, user, PermissionLevel.READ);

        List<WorkflowExecution> executions = executionRepository.findByWorkflowIdOrderByStartedAtDesc(id);
        return workflowMapper.toDTOWithExecutions(workflow, executions);
    }

    public WorkflowDTO createWorkflow(CreateWorkflowRequest request, String userEmail) {
        logger.info("Creating new workflow: {} for user: {}", request.getName(), userEmail);

        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            User user = getUser(userEmail);
            Project project = projectRepository.findById(new ObjectId(request.getProjectId()))
                .orElseThrow(() -> new RuntimeException("Project not found"));
            if (!project.hasAccess(user, PermissionLevel.EDITOR)) {
                throw new RuntimeException("Access denied to create workflow in this project");
            }
        }

        Workflow workflow = new Workflow();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setCreatedBy(userEmail);
        workflow.setIsTemplate(request.getIsTemplate() != null ? request.getIsTemplate() : false);
        workflow.setStatus(WorkflowStatus.DRAFT);
        
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            workflow.setProjectId(new ObjectId(request.getProjectId()));
        }

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

    public WorkflowDTO copyWorkflow(String originalWorkflowId, String targetProjectId, String userEmail) {
        logger.info("Copying workflow: {} to project: {} for user: {}", originalWorkflowId, targetProjectId, userEmail);

        Workflow originalWorkflow = workflowRepository.findById(originalWorkflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
            
        User user = getUser(userEmail);
        if (!originalWorkflow.getIsTemplate()) {
            checkProjectAccess(originalWorkflow, user, PermissionLevel.READ);
        }

        if (targetProjectId != null && !targetProjectId.isEmpty()) {
            Project targetProject = projectRepository.findById(new ObjectId(targetProjectId))
                .orElseThrow(() -> new RuntimeException("Target project not found"));
            if (!targetProject.hasAccess(user, PermissionLevel.EDITOR)) {
                throw new RuntimeException("Access denied to copy workflow into target project");
            }
        }

        Workflow newWorkflow = new Workflow();
        newWorkflow.setName(originalWorkflow.getName() + " (Copy)");
        newWorkflow.setDescription(originalWorkflow.getDescription());
        newWorkflow.setCreatedBy(userEmail);
        newWorkflow.setIsTemplate(false);
        newWorkflow.setStatus(WorkflowStatus.DRAFT);
        newWorkflow.setCurrentVersion(originalWorkflow.getCurrentVersion());

        if (targetProjectId != null && !targetProjectId.isEmpty()) {
            newWorkflow.setProjectId(new ObjectId(targetProjectId));
        }

        List<WorkflowVersion> copiedVersions = new ArrayList<>();
        for (WorkflowVersion pv : originalWorkflow.getVersions()) {
            WorkflowVersion v = new WorkflowVersion();
            v.setVersion(pv.getVersion());
            v.setCreatedAt(pv.getCreatedAt());
            v.setCreatedBy(pv.getCreatedBy());
            v.setNodes(new ArrayList<>(pv.getNodes()));
            v.setEdges(new ArrayList<>(pv.getEdges()));
            v.setChangelog(pv.getChangelog() != null ? pv.getChangelog() : "Imported version");
            copiedVersions.add(v);
        }

        newWorkflow.setVersions(copiedVersions);
        newWorkflow.setCreatedAt(LocalDateTime.now());
        newWorkflow.setUpdatedAt(LocalDateTime.now());

        Workflow savedWorkflow = workflowRepository.save(newWorkflow);
        return workflowMapper.toDTO(savedWorkflow);
    }

    public WorkflowDTO updateWorkflow(String id, UpdateWorkflowRequest request, String userEmail) {
        logger.info("Updating workflow: {} for user: {}", id, userEmail);

        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
            
        User user = getUser(userEmail);
        checkProjectAccess(workflow, user, PermissionLevel.EDITOR);

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

        Workflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workflow not found"));
            
        User user = getUser(userEmail);
        checkProjectAccess(workflow, user, PermissionLevel.EDITOR);

        List<WorkflowExecution> executions = executionRepository.findByWorkflowId(id);
        executionRepository.deleteAll(executions);

        workflowRepository.delete(workflow);
        logger.info("Workflow deleted: {}", id);
    }

    private String generateNextVersion(String currentVersion) {
        String numericPart = currentVersion.substring(1);
        String[] parts = numericPart.split("\\.");
        int minor = Integer.parseInt(parts[1]) + 1;
        return "v" + parts[0] + "." + minor;
    }
}
