package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.services.ConductorRegistrationService;
import com.enit.satellite_platform.modules.workflow.services.WorkflowDefinitionService;
import com.enit.satellite_platform.modules.workflow.services.WorkflowExecutionService;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Workflow Definition management
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {
    
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ConductorRegistrationService conductorRegistrationService;
    private final WorkflowExecutionService workflowExecutionService;
    
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
     * Register workflow with Conductor
     */
    @PostMapping("/{id}/register")
    public ResponseEntity<Map<String, String>> registerWorkflowWithConductor(@PathVariable String id) {
        WorkflowResponse workflow = workflowDefinitionService.getWorkflowById(id);
        
        // Convert response back to entity for registration (simple mapping)
        com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition workflowEntity = 
                new com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition();
        workflowEntity.setId(workflow.getId());
        workflowEntity.setName(workflow.getName());
        workflowEntity.setDescription(workflow.getDescription());
        workflowEntity.setProjectId(workflow.getProjectId());
        workflowEntity.setVersion(workflow.getVersion());
        workflowEntity.setStatus(workflow.getStatus());
        workflowEntity.setCreatedBy(workflow.getCreatedBy());
        workflowEntity.setCreatedAt(workflow.getCreatedAt());
        workflowEntity.setUpdatedAt(workflow.getUpdatedAt());
        workflowEntity.setNodes(workflow.getNodes());
        workflowEntity.setEdges(workflow.getEdges());
        
        com.enit.satellite_platform.modules.workflow.entities.WorkflowMetadata metadata = 
                new com.enit.satellite_platform.modules.workflow.entities.WorkflowMetadata();
        metadata.setTimeoutSeconds(workflow.getTimeoutSeconds());
        metadata.setTags(workflow.getTags());
        workflowEntity.setMetadata(metadata);
        
        conductorRegistrationService.registerWorkflow(workflowEntity);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Workflow registered successfully with Conductor");
        response.put("workflowId", id);
        response.put("status", "registered");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Conductor workflow definition
     */
    @GetMapping("/{id}/conductor-def")
    public ResponseEntity<WorkflowDef> getConductorWorkflowDef(@PathVariable String id) {
        WorkflowResponse workflow = workflowDefinitionService.getWorkflowById(id);
        
        String conductorWorkflowName = workflow.getProjectId() + "_" + 
                workflow.getName().toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        
        WorkflowDef conductorDef = conductorRegistrationService.getWorkflowDef(conductorWorkflowName, null);
        
        if (conductorDef == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(conductorDef);
    }
    
    /**
     * Execute/start a workflow
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> input) {
        WorkflowResponse workflow = workflowDefinitionService.getWorkflowById(id);
        
        String conductorWorkflowName = workflow.getProjectId() + "_" + 
                workflow.getName().toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        
        // Default input if not provided
        if (input == null) {
            input = new HashMap<>();
        }
        
        String workflowId = workflowExecutionService.startWorkflow(conductorWorkflowName, 1, input);
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("conductorWorkflowName", conductorWorkflowName);
        response.put("status", "started");
        response.put("message", "Workflow execution started successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get workflow execution status
     */
    @GetMapping("/execution/{workflowId}/status")
    public ResponseEntity<Map<String, Object>> getExecutionStatus(@PathVariable String workflowId) {
        Map<String, Object> status = workflowExecutionService.getWorkflowStatus(workflowId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get workflow execution details with tasks
     */
    @GetMapping("/execution/{workflowId}/details")
    public ResponseEntity<Map<String, Object>> getExecutionDetails(@PathVariable String workflowId) {
        Map<String, Object> details = workflowExecutionService.getWorkflowDetails(workflowId);
        return ResponseEntity.ok(details);
    }
    
    /**
     * Terminate a workflow execution
     */
    @PostMapping("/execution/{workflowId}/terminate")
    public ResponseEntity<Map<String, String>> terminateExecution(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Terminated by user";
        workflowExecutionService.terminateWorkflow(workflowId, reason);
        
        Map<String, String> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("status", "terminated");
        response.put("message", "Workflow terminated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Pause a workflow execution
     */
    @PostMapping("/execution/{workflowId}/pause")
    public ResponseEntity<Map<String, String>> pauseExecution(@PathVariable String workflowId) {
        workflowExecutionService.pauseWorkflow(workflowId);
        
        Map<String, String> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("status", "paused");
        response.put("message", "Workflow paused successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Resume a paused workflow execution
     */
    @PostMapping("/execution/{workflowId}/resume")
    public ResponseEntity<Map<String, String>> resumeExecution(@PathVariable String workflowId) {
        workflowExecutionService.resumeWorkflow(workflowId);
        
        Map<String, String> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("status", "resumed");
        response.put("message", "Workflow resumed successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Restart a workflow execution
     */
    @PostMapping("/execution/{workflowId}/restart")
    public ResponseEntity<Map<String, String>> restartExecution(@PathVariable String workflowId) {
        workflowExecutionService.restartWorkflow(workflowId);
        
        Map<String, String> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("status", "restarted");
        response.put("message", "Workflow restarted successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        return authentication != null ? authentication.getName() : "system";
    }

}
