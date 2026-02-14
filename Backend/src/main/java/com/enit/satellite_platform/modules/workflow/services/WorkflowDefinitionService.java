package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.CreateWorkflowRequest;
import com.enit.satellite_platform.modules.workflow.dto.UpdateWorkflowRequest;
import com.enit.satellite_platform.modules.workflow.dto.WorkflowResponse;
import com.enit.satellite_platform.modules.workflow.entities.Workflow;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowVersion;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService executionService;
    
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request, String userId) {
        Workflow workflow = new Workflow();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setStatus("DRAFT");
        workflow.setProjectId(request.getProjectId());
        workflow.setCurrentVersion("1.0.0");
        workflow.setCreatedBy(userId);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflow.setTags(request.getTags());
        workflow.setTemplate(request.isTemplate());
        
        // Create initial version
        WorkflowVersion version = new WorkflowVersion();
        version.setVersion("1.0.0");
        version.setNodes(request.getNodes());
        version.setEdges(request.getEdges());
        version.setChangelog("Initial version");
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        
        List<WorkflowVersion> versions = new ArrayList<>();
        versions.add(version);
        workflow.setVersions(versions);
        
        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }
    
    public WorkflowResponse getWorkflowById(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        return toResponse(workflow);
    }
    
    public List<WorkflowResponse> getAllWorkflows() {
        return workflowRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<WorkflowResponse> getWorkflowsByProject(String projectId) {
        return workflowRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<WorkflowResponse> getTemplates() {
        return workflowRepository.findByIsTemplate(true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public WorkflowResponse updateWorkflow(String id, UpdateWorkflowRequest request, String userId) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }
        if (request.getTags() != null) {
            workflow.setTags(request.getTags());
        }
        
        // Create new version if nodes or edges changed
        if (request.getNodes() != null || request.getEdges() != null) {
            String newVersion = incrementVersion(workflow.getCurrentVersion());
            
            WorkflowVersion version = new WorkflowVersion();
            version.setVersion(newVersion);
            version.setNodes(request.getNodes() != null ? request.getNodes() : getCurrentVersion(workflow).getNodes());
            version.setEdges(request.getEdges() != null ? request.getEdges() : getCurrentVersion(workflow).getEdges());
            version.setChangelog(request.getChangelog() != null ? request.getChangelog() : "Updated workflow");
            version.setCreatedBy(userId);
            version.setCreatedAt(LocalDateTime.now());
            
            workflow.getVersions().add(version);
            workflow.setCurrentVersion(newVersion);
        }
        
        workflow.setUpdatedAt(LocalDateTime.now());
        Workflow updated = workflowRepository.save(workflow);
        return toResponse(updated);
    }
    
    public void deleteWorkflow(String id) {
        workflowRepository.deleteById(id);
    }
    
    private WorkflowVersion getCurrentVersion(Workflow workflow) {
        return workflow.getVersions().stream()
                .filter(v -> v.getVersion().equals(workflow.getCurrentVersion()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Current version not found"));
    }
    
    private String incrementVersion(String currentVersion) {
        String[] parts = currentVersion.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }
    
    private WorkflowResponse toResponse(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(workflow.getId());
        response.setName(workflow.getName());
        response.setDescription(workflow.getDescription());
        response.setStatus(workflow.getStatus());
        response.setProjectId(workflow.getProjectId());
        response.setCurrentVersion(workflow.getCurrentVersion());
        response.setVersions(workflow.getVersions());
        response.setExecutions(executionService.getExecutionsByWorkflowId(workflow.getId()));
        response.setCreatedBy(workflow.getCreatedBy());
        response.setCreatedAt(workflow.getCreatedAt());
        response.setUpdatedAt(workflow.getUpdatedAt());
        response.setTags(workflow.getTags());
        response.setIsTemplate(workflow.isTemplate());
        return response;
    }
}
