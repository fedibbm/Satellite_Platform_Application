package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowMetadata;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing workflow definitions (CRUD operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {
    
    private final WorkflowDefinitionRepository workflowRepository;
    
    /**
     * Create a new workflow definition
     */
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request, String userId) {
        log.info("Creating new workflow: {} for project: {}", request.getName(), request.getProjectId());
        
        // Check if workflow with same name exists in project
        workflowRepository.findByNameAndProjectId(request.getName(), request.getProjectId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Workflow with name '" + request.getName() + 
                            "' already exists in project " + request.getProjectId());
                });
        
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setProjectId(request.getProjectId());
        workflow.setVersion("1.0");
        workflow.setStatus("DRAFT");
        workflow.setCreatedBy(userId);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());
        workflow.setNodes(request.getNodes());
        workflow.setEdges(request.getEdges());
        
        // Set metadata
        WorkflowMetadata metadata = new WorkflowMetadata();
        metadata.setTimeoutSeconds(request.getTimeoutSeconds());
        metadata.setTags(request.getTags());
        metadata.setSchemaVersion("1.0");
        metadata.setRestartable(true);
        workflow.setMetadata(metadata);
        
        WorkflowDefinition saved = workflowRepository.save(workflow);
        log.info("Workflow created successfully with id: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    /**
     * Get workflow by ID
     */
    public WorkflowResponse getWorkflowById(String id) {
        WorkflowDefinition workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        return toResponse(workflow);
    }
    
    /**
     * Get all workflows
     */
    public List<WorkflowResponse> getAllWorkflows() {
        return workflowRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get workflows by project ID
     */
    public List<WorkflowResponse> getWorkflowsByProject(String projectId) {
        return workflowRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get workflows by status
     */
    public List<WorkflowResponse> getWorkflowsByStatus(String status) {
        return workflowRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update workflow
     */
    public WorkflowResponse updateWorkflow(String id, UpdateWorkflowRequest request) {
        log.info("Updating workflow: {}", id);
        
        WorkflowDefinition workflow = workflowRepository.findById(id)
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
        if (request.getNodes() != null) {
            workflow.setNodes(request.getNodes());
            log.info("Updating workflow nodes: {} nodes", request.getNodes().size());
        }
        if (request.getEdges() != null) {
            workflow.setEdges(request.getEdges());
            log.info("Updating workflow edges: {} edges", request.getEdges().size());
        }
        
        workflow.setUpdatedAt(LocalDateTime.now());
        
        WorkflowDefinition updated = workflowRepository.save(workflow);
        log.info("Workflow updated successfully: {}", id);
        
        return toResponse(updated);
    }
    
    /**
     * Delete workflow
     */
    public void deleteWorkflow(String id) {
        log.info("Deleting workflow: {}", id);
        
        if (!workflowRepository.existsById(id)) {
            throw new RuntimeException("Workflow not found with id: " + id);
        }
        
        workflowRepository.deleteById(id);
        log.info("Workflow deleted successfully: {}", id);
    }
    
    /**
     * Publish workflow (change status to PUBLISHED)
     */
    public WorkflowResponse publishWorkflow(String id) {
        log.info("Publishing workflow: {}", id);
        
        WorkflowDefinition workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));
        
        workflow.setStatus("PUBLISHED");
        workflow.setUpdatedAt(LocalDateTime.now());
        
        WorkflowDefinition published = workflowRepository.save(workflow);
        log.info("Workflow published successfully: {}", id);
        
        return toResponse(published);
    }
    
    /**
     * Convert entity to response DTO
     */
    private WorkflowResponse toResponse(WorkflowDefinition workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(workflow.getId());
        response.setName(workflow.getName());
        response.setDescription(workflow.getDescription());
        response.setProjectId(workflow.getProjectId());
        response.setVersion(workflow.getVersion());
        response.setStatus(workflow.getStatus());
        response.setCreatedBy(workflow.getCreatedBy());
        response.setCreatedAt(workflow.getCreatedAt());
        response.setUpdatedAt(workflow.getUpdatedAt());
        response.setNodes(workflow.getNodes());
        response.setEdges(workflow.getEdges());
        
        if (workflow.getMetadata() != null) {
            response.setTimeoutSeconds(workflow.getMetadata().getTimeoutSeconds());
            response.setTags(workflow.getMetadata().getTags());
        }
        
        return response;
    }
}
