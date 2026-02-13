package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import com.enit.satellite_platform.modules.workflow.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {
    
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowOrchestrationService orchestrationService;
    private final NodeRegistryService nodeRegistryService;
    
    // ============= Workflow CRUD Operations =============
    
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @RequestBody CreateWorkflowRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        WorkflowResponse response = workflowDefinitionService.createWorkflow(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        List<WorkflowResponse> workflows = workflowDefinitionService.getAllWorkflows();
        return ResponseEntity.ok(workflows);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable String id) {
        WorkflowResponse workflow = workflowDefinitionService.getWorkflowById(id);
        return ResponseEntity.ok(workflow);
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WorkflowResponse>> getWorkflowsByProject(@PathVariable String projectId) {
        List<WorkflowResponse> workflows = workflowDefinitionService.getWorkflowsByProject(projectId);
        return ResponseEntity.ok(workflows);
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<WorkflowResponse>> getTemplates() {
        List<WorkflowResponse> templates = workflowDefinitionService.getTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable String id,
            @RequestBody UpdateWorkflowRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        WorkflowResponse response = workflowDefinitionService.updateWorkflow(id, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        workflowDefinitionService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }
    
    // ============= Workflow Execution Operations =============
    
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, String>> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) ExecuteWorkflowRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        
        // Start async execution
        orchestrationService.executeWorkflow(
                id, 
                userId, 
                request != null ? request.getParameters() : null
        );
        
        return ResponseEntity.accepted().body(Map.of(
                "message", "Workflow execution started",
                "workflowId", id
        ));
    }
    
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<WorkflowExecution>> getWorkflowExecutions(@PathVariable String id) {
        List<WorkflowExecution> executions = workflowExecutionService.getExecutionsByWorkflowId(id);
        return ResponseEntity.ok(executions);
    }
    
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<WorkflowExecution> getExecutionById(@PathVariable String executionId) {
        WorkflowExecution execution = workflowExecutionService.getExecutionById(executionId);
        return ResponseEntity.ok(execution);
    }
    
    @GetMapping("/executions")
    public ResponseEntity<List<WorkflowExecution>> getAllExecutions() {
        List<WorkflowExecution> executions = workflowExecutionService.getAllExecutions();
        return ResponseEntity.ok(executions);
    }
    
    @PostMapping("/executions/{executionId}/cancel")
    public ResponseEntity<WorkflowExecution> cancelExecution(@PathVariable String executionId) {
        WorkflowExecution execution = workflowExecutionService.updateExecutionStatus(executionId, "cancelled");
        return ResponseEntity.ok(execution);
    }
    
    // ============= Node Registry Operations =============
    
    @GetMapping("/nodes/types")
    public ResponseEntity<List<NodeRegistryService.NodeMetadata>> getNodeTypes() {
        List<NodeRegistryService.NodeMetadata> nodeTypes = nodeRegistryService.getAllNodeTypes();
        return ResponseEntity.ok(nodeTypes);
    }
    
    @GetMapping("/nodes/types/{type}")
    public ResponseEntity<NodeRegistryService.NodeMetadata> getNodeType(@PathVariable String type) {
        NodeRegistryService.NodeMetadata metadata = nodeRegistryService.getNodeMetadata(type);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }
    
    // ============= Helper Methods =============
    
    private String getUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system"; // Default user for testing
    }
}
