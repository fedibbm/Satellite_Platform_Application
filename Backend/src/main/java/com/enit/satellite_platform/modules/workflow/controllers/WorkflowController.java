package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.dto.*;
import com.enit.satellite_platform.modules.workflow.mapper.WorkflowMapper;
import com.enit.satellite_platform.modules.workflow.services.WorkflowExecutionService;
import com.enit.satellite_platform.modules.workflow.services.WorkflowService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
@Tag(name = "Workflow Controller", description = "Endpoints for managing workflows and executions")
public class WorkflowController {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowExecutionService executionService;

    @Autowired
    private WorkflowMapper workflowMapper;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    @GetMapping
    @Operation(summary = "Get all workflows for current user")
    public ResponseEntity<GenericResponse<List<WorkflowDTO>>> getAllWorkflows() {
        try {
            String userEmail = getCurrentUserEmail();
            List<WorkflowDTO> workflows = workflowService.getAllWorkflows(userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflows retrieved successfully", workflows));
        } catch (Exception e) {
            logger.error("Error fetching workflows", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/templates")
    @Operation(summary = "Get all workflow templates")
    public ResponseEntity<GenericResponse<List<WorkflowDTO>>> getWorkflowTemplates() {
        try {
            List<WorkflowDTO> templates = workflowService.getWorkflowTemplates();
            return ResponseEntity.ok(new GenericResponse<>("success", "Templates retrieved successfully", templates));
        } catch (Exception e) {
            logger.error("Error fetching templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get workflows by project ID")
    public ResponseEntity<GenericResponse<List<WorkflowDTO>>> getWorkflowsByProject(@PathVariable String projectId) {
        try {
            String userEmail = getCurrentUserEmail();
            List<WorkflowDTO> workflows = workflowService.getWorkflowsByProject(projectId, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflows retrieved successfully", workflows));
        } catch (Exception e) {
            logger.error("Error fetching workflows for project: {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    public ResponseEntity<GenericResponse<WorkflowDTO>> getWorkflowById(@PathVariable String id) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowDTO workflow = workflowService.getWorkflowById(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflow retrieved successfully", workflow));
        } catch (Exception e) {
            logger.error("Error fetching workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new workflow")
    public ResponseEntity<GenericResponse<WorkflowDTO>> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowDTO workflow = workflowService.createWorkflow(request, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse<>("success", "Workflow created successfully", workflow));
        } catch (Exception e) {
            logger.error("Error creating workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "Copy an existing workflow")
    public ResponseEntity<GenericResponse<WorkflowDTO>> copyWorkflow(
            @PathVariable String id,
            @RequestParam(required = false) String targetProjectId) {
        try {
            String userEmail = getCurrentUserEmail();
            // Call service copy method
            WorkflowDTO workflow = workflowService.copyWorkflow(id, targetProjectId, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse<>("success", "Workflow copied successfully", workflow));
        } catch (Exception e) {
            logger.error("Error copying workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a workflow")
    public ResponseEntity<GenericResponse<WorkflowDTO>> updateWorkflow(
            @PathVariable String id,
            @RequestBody UpdateWorkflowRequest request) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowDTO workflow = workflowService.updateWorkflow(id, request, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflow updated successfully", workflow));
        } catch (Exception e) {
            logger.error("Error updating workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow")
    public ResponseEntity<GenericResponse<Void>> deleteWorkflow(@PathVariable String id) {
        try {
            String userEmail = getCurrentUserEmail();
            workflowService.deleteWorkflow(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflow deleted successfully", null));
        } catch (Exception e) {
            logger.error("Error deleting workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute a workflow")
    public ResponseEntity<GenericResponse<WorkflowExecutionDTO>> executeWorkflow(@PathVariable String id) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowExecutionDTO execution = executionService.executeWorkflow(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Workflow execution started", execution));
        } catch (Exception e) {
            logger.error("Error executing workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Get all executions for a workflow")
    public ResponseEntity<GenericResponse<List<WorkflowExecutionDTO>>> getWorkflowExecutions(@PathVariable String id) {
        try {
            String userEmail = getCurrentUserEmail();
            List<WorkflowExecutionDTO> executions = executionService.getWorkflowExecutions(id, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Executions retrieved successfully", executions));
        } catch (Exception e) {
            logger.error("Error fetching executions for workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/executions/{executionId}")
    @Operation(summary = "Get execution by ID")
    public ResponseEntity<GenericResponse<WorkflowExecutionDTO>> getExecutionById(@PathVariable String executionId) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowExecutionDTO execution = executionService.getExecutionById(executionId, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success", "Execution retrieved successfully", execution));
        } catch (Exception e) {
            logger.error("Error fetching execution: {}", executionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get all versions of a workflow")
    public ResponseEntity<GenericResponse<List<WorkflowVersionDTO>>> getWorkflowVersions(@PathVariable String id) {
        try {
            String userEmail = getCurrentUserEmail();
            WorkflowDTO workflow = workflowService.getWorkflowById(id, userEmail);
            String currentVersion = workflow.getCurrentVersion();
            List<WorkflowVersionDTO> versions = workflow.getVersions().stream()
                .map(v -> workflowMapper.toVersionDTO(v, currentVersion))
                .toList();
            return ResponseEntity.ok(new GenericResponse<>("success", "Versions retrieved successfully", versions));
        } catch (Exception e) {
            logger.error("Error fetching versions for workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/revert")
    @Operation(summary = "Revert workflow to a previous version")
    public ResponseEntity<GenericResponse<WorkflowDTO>> revertToVersion(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String userEmail = getCurrentUserEmail();
            String targetVersion = request.get("version");
            if (targetVersion == null || targetVersion.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new GenericResponse<>("error", "'version' field is required", null));
            }
            WorkflowDTO workflow = workflowService.revertToVersion(id, targetVersion, userEmail);
            return ResponseEntity.ok(new GenericResponse<>("success",
                    "Workflow reverted to " + targetVersion + " successfully", workflow));
        } catch (Exception e) {
            logger.error("Error reverting workflow: {} to version: {}", id, request.get("version"), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse<>("error", e.getMessage(), null));
        }
    }
}
