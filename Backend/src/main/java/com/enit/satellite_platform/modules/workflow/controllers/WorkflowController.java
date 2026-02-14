package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.services.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Workflow Definition management
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {
    
    private final WorkflowDefinitionService workflowDefinitionService;
    
    /**
     * Create a new workflow
     */
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @RequestBody CreateWorkflowRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        WorkflowResponse response = workflowDefinitionService.createWorkflow(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all workflows
     */
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        List<WorkflowResponse> workflows = workflowDefinitionService.getAllWorkflows();
        return ResponseEntity.ok(workflows);
    }
    
    /**
     * Get workflow by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable String id) {
        WorkflowResponse workflow = workflowDefinitionService.getWorkflowById(id);
        return ResponseEntity.ok(workflow);
    }
    
    /**
     * Get workflows by project ID
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WorkflowResponse>> getWorkflowsByProject(@PathVariable String projectId) {
        List<WorkflowResponse> workflows = workflowDefinitionService.getWorkflowsByProject(projectId);
        return ResponseEntity.ok(workflows);
    }
    
    /**
     * Get workflows by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<WorkflowResponse>> getWorkflowsByStatus(@PathVariable String status) {
        List<WorkflowResponse> workflows = workflowDefinitionService.getWorkflowsByStatus(status);
        return ResponseEntity.ok(workflows);
    }
    
    /**
     * Update workflow
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable String id,
            @RequestBody UpdateWorkflowRequest request) {
        WorkflowResponse workflow = workflowDefinitionService.updateWorkflow(id, request);
        return ResponseEntity.ok(workflow);
    }
    
    /**
     * Delete workflow
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        workflowDefinitionService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Publish workflow
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<WorkflowResponse> publishWorkflow(@PathVariable String id) {
        WorkflowResponse workflow = workflowDefinitionService.publishWorkflow(id);
        return ResponseEntity.ok(workflow);
    }
    
    /**
     * Extract user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        return authentication != null ? authentication.getName() : "system";
    }
}
