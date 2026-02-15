package com.enit.satellite_platform.modules.workflow.controllers;

import com.enit.satellite_platform.modules.workflow.dto.CreateTriggerRequest;
import com.enit.satellite_platform.modules.workflow.dto.TriggerResponse;
import com.enit.satellite_platform.modules.workflow.dto.UpdateTriggerRequest;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import com.enit.satellite_platform.modules.workflow.services.TriggerManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing workflow triggers
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/triggers")
@RequiredArgsConstructor
public class TriggerController {
    
    private final TriggerManagementService triggerManagementService;
    
    /**
     * Create a new workflow trigger
     * POST /api/workflow/triggers
     */
    @PostMapping
    public ResponseEntity<TriggerResponse> createTrigger(
            @RequestBody CreateTriggerRequest request,
            Authentication authentication) {
        
        try {
            log.info("Creating workflow trigger: name={}, type={}", request.getName(), request.getType());
            
            String userId = authentication != null ? authentication.getName() : "system";
            
            WorkflowTrigger trigger = triggerManagementService.createTrigger(
                    request.getWorkflowDefinitionId(),
                    request.getProjectId(),
                    request.getName(),
                    request.getDescription(),
                    request.getType(),
                    request.getConfig(),
                    request.getDefaultInputs(),
                    userId
            );
            
            TriggerResponse response = TriggerResponse.fromEntity(trigger);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger creation request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating workflow trigger", e);
            throw new RuntimeException("Failed to create workflow trigger: " + e.getMessage());
        }
    }
    
    /**
     * Get trigger by ID
     * GET /api/workflow/triggers/{triggerId}
     */
    @GetMapping("/{triggerId}")
    public ResponseEntity<TriggerResponse> getTrigger(@PathVariable String triggerId) {
        try {
            WorkflowTrigger trigger = triggerManagementService.getTrigger(triggerId);
            TriggerResponse response = TriggerResponse.fromEntity(trigger);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Trigger not found: id={}", triggerId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all triggers for a project
     * GET /api/workflow/triggers/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TriggerResponse>> getProjectTriggers(@PathVariable String projectId) {
        try {
            List<WorkflowTrigger> triggers = triggerManagementService.getProjectTriggers(projectId);
            List<TriggerResponse> responses = triggers.stream()
                    .map(TriggerResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error getting project triggers: projectId={}", projectId, e);
            throw new RuntimeException("Failed to get project triggers: " + e.getMessage());
        }
    }
    
    /**
     * Get all triggers for a workflow definition
     * GET /api/workflow/triggers/workflow/{workflowDefinitionId}
     */
    @GetMapping("/workflow/{workflowDefinitionId}")
    public ResponseEntity<List<TriggerResponse>> getWorkflowTriggers(
            @PathVariable String workflowDefinitionId) {
        
        try {
            List<WorkflowTrigger> triggers = triggerManagementService
                    .getWorkflowTriggers(workflowDefinitionId);
            
            List<TriggerResponse> responses = triggers.stream()
                    .map(TriggerResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error getting workflow triggers: workflowId={}", workflowDefinitionId, e);
            throw new RuntimeException("Failed to get workflow triggers: " + e.getMessage());
        }
    }
    
    /**
     * Get all triggers by type
     * GET /api/workflow/triggers/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<TriggerResponse>> getTriggersByType(
            @PathVariable WorkflowTrigger.TriggerType type) {
        
        try {
            List<WorkflowTrigger> triggers = triggerManagementService.getTriggersByType(type);
            List<TriggerResponse> responses = triggers.stream()
                    .map(TriggerResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error getting triggers by type: type={}", type, e);
            throw new RuntimeException("Failed to get triggers by type: " + e.getMessage());
        }
    }
    
    /**
     * Update a trigger
     * PUT /api/workflow/triggers/{triggerId}
     */
    @PutMapping("/{triggerId}")
    public ResponseEntity<TriggerResponse> updateTrigger(
            @PathVariable String triggerId,
            @RequestBody UpdateTriggerRequest request) {
        
        try {
            log.info("Updating workflow trigger: id={}", triggerId);
            
            WorkflowTrigger trigger = triggerManagementService.updateTrigger(
                    triggerId,
                    request.getName(),
                    request.getDescription(),
                    request.getConfig(),
                    request.getDefaultInputs(),
                    request.getEnabled()
            );
            
            TriggerResponse response = TriggerResponse.fromEntity(trigger);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger update: id={}, error={}", triggerId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating trigger: id={}", triggerId, e);
            throw new RuntimeException("Failed to update trigger: " + e.getMessage());
        }
    }
    
    /**
     * Delete a trigger
     * DELETE /api/workflow/triggers/{triggerId}
     */
    @DeleteMapping("/{triggerId}")
    public ResponseEntity<Map<String, Object>> deleteTrigger(@PathVariable String triggerId) {
        try {
            log.info("Deleting workflow trigger: id={}", triggerId);
            
            triggerManagementService.deleteTrigger(triggerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Trigger deleted successfully");
            response.put("triggerId", triggerId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Trigger not found for deletion: id={}", triggerId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting trigger: id={}", triggerId, e);
            throw new RuntimeException("Failed to delete trigger: " + e.getMessage());
        }
    }
    
    /**
     * Enable a trigger
     * POST /api/workflow/triggers/{triggerId}/enable
     */
    @PostMapping("/{triggerId}/enable")
    public ResponseEntity<TriggerResponse> enableTrigger(@PathVariable String triggerId) {
        try {
            log.info("Enabling workflow trigger: id={}", triggerId);
            
            WorkflowTrigger trigger = triggerManagementService.enableTrigger(triggerId);
            TriggerResponse response = TriggerResponse.fromEntity(trigger);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Trigger not found: id={}", triggerId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Disable a trigger
     * POST /api/workflow/triggers/{triggerId}/disable
     */
    @PostMapping("/{triggerId}/disable")
    public ResponseEntity<TriggerResponse> disableTrigger(@PathVariable String triggerId) {
        try {
            log.info("Disabling workflow trigger: id={}", triggerId);
            
            WorkflowTrigger trigger = triggerManagementService.disableTrigger(triggerId);
            TriggerResponse response = TriggerResponse.fromEntity(trigger);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Trigger not found: id={}", triggerId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get trigger statistics
     * GET /api/workflow/triggers/{triggerId}/stats
     */
    @GetMapping("/{triggerId}/stats")
    public ResponseEntity<Map<String, Object>> getTriggerStatistics(@PathVariable String triggerId) {
        try {
            Map<String, Object> stats = triggerManagementService.getTriggerStatistics(triggerId);
            return ResponseEntity.ok(stats);
            
        } catch (IllegalArgumentException e) {
            log.warn("Trigger not found: id={}", triggerId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all enabled triggers
     * GET /api/workflow/triggers/enabled
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<TriggerResponse>> getEnabledTriggers() {
        try {
            List<WorkflowTrigger> triggers = triggerManagementService.getEnabledTriggers();
            List<TriggerResponse> responses = triggers.stream()
                    .map(TriggerResponse::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("Error getting enabled triggers", e);
            throw new RuntimeException("Failed to get enabled triggers: " + e.getMessage());
        }
    }
}
