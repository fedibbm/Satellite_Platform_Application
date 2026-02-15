package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.TriggerConfig;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing workflow triggers
 * Handles CRUD operations and trigger lifecycle
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerManagementService {
    
    private final WorkflowTriggerRepository triggerRepository;
    private final ScheduledTriggerService scheduledTriggerService;
    
    /**
     * Create a new workflow trigger
     * 
     * @param workflowDefinitionId Workflow to trigger
     * @param projectId Project ID
     * @param name Trigger name
     * @param description Trigger description
     * @param type Trigger type
     * @param config Trigger configuration
     * @param defaultInputs Default workflow inputs
     * @param createdBy User ID
     * @return Created trigger
     */
    @Transactional
    public WorkflowTrigger createTrigger(
            String workflowDefinitionId,
            String projectId,
            String name,
            String description,
            WorkflowTrigger.TriggerType type,
            TriggerConfig config,
            Map<String, Object> defaultInputs,
            String createdBy) {
        
        log.info("Creating workflow trigger: name={}, type={}, projectId={}", 
                name, type, projectId);
        
        // Validate inputs
        validateTriggerCreation(workflowDefinitionId, projectId, name, type, config);
        
        // Check for duplicate name in project
        Optional<WorkflowTrigger> existing = triggerRepository.findByProjectIdAndName(projectId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Trigger with name '" + name + "' already exists in this project");
        }
        
        // Create trigger
        WorkflowTrigger trigger = new WorkflowTrigger();
        trigger.setName(name);
        trigger.setDescription(description);
        trigger.setWorkflowDefinitionId(workflowDefinitionId);
        trigger.setProjectId(projectId);
        trigger.setType(type);
        trigger.setConfig(config);
        trigger.setDefaultInputs(defaultInputs);
        trigger.setEnabled(true);
        trigger.setCreatedBy(createdBy);
        trigger.setCreatedAt(LocalDateTime.now());
        trigger.setUpdatedAt(LocalDateTime.now());
        trigger.setExecutionCount(0L);
        
        // Generate webhook secret for webhook triggers
        if (type == WorkflowTrigger.TriggerType.WEBHOOK && 
            (config.getWebhookSecret() == null || config.getWebhookSecret().isEmpty())) {
            config.setWebhookSecret(generateWebhookSecret());
        }
        
        WorkflowTrigger saved = triggerRepository.save(trigger);
        
        log.info("Created workflow trigger: id={}, name={}, type={}", 
                saved.getId(), saved.getName(), saved.getType());
        
        return saved;
    }
    
    /**
     * Update an existing trigger
     */
    @Transactional
    public WorkflowTrigger updateTrigger(
            String triggerId,
            String name,
            String description,
            TriggerConfig config,
            Map<String, Object> defaultInputs,
            Boolean enabled) {
        
        log.info("Updating workflow trigger: id={}", triggerId);
        
        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + triggerId));
        
        // Update fields
        if (name != null) {
            // Check for duplicate name
            Optional<WorkflowTrigger> existing = triggerRepository
                    .findByProjectIdAndName(trigger.getProjectId(), name);
            if (existing.isPresent() && !existing.get().getId().equals(triggerId)) {
                throw new IllegalArgumentException("Trigger with name '" + name + "' already exists");
            }
            trigger.setName(name);
        }
        
        if (description != null) {
            trigger.setDescription(description);
        }
        
        if (config != null) {
            // Validate config for scheduled triggers
            if (trigger.getType() == WorkflowTrigger.TriggerType.SCHEDULED && 
                config.getCronExpression() != null) {
                if (!scheduledTriggerService.validateCronExpression(config.getCronExpression())) {
                    throw new IllegalArgumentException("Invalid cron expression: " + config.getCronExpression());
                }
            }
            trigger.setConfig(config);
        }
        
        if (defaultInputs != null) {
            trigger.setDefaultInputs(defaultInputs);
        }
        
        if (enabled != null) {
            trigger.setEnabled(enabled);
        }
        
        trigger.setUpdatedAt(LocalDateTime.now());
        
        WorkflowTrigger updated = triggerRepository.save(trigger);
        
        log.info("Updated workflow trigger: id={}, name={}", updated.getId(), updated.getName());
        
        return updated;
    }
    
    /**
     * Delete a trigger
     */
    @Transactional
    public void deleteTrigger(String triggerId) {
        log.info("Deleting workflow trigger: id={}", triggerId);
        
        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + triggerId));
        
        triggerRepository.delete(trigger);
        
        log.info("Deleted workflow trigger: id={}, name={}", triggerId, trigger.getName());
    }
    
    /**
     * Get trigger by ID
     */
    public WorkflowTrigger getTrigger(String triggerId) {
        return triggerRepository.findById(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + triggerId));
    }
    
    /**
     * Get all triggers for a project
     */
    public List<WorkflowTrigger> getProjectTriggers(String projectId) {
        return triggerRepository.findByProjectId(projectId);
    }
    
    /**
     * Get all triggers for a workflow definition
     */
    public List<WorkflowTrigger> getWorkflowTriggers(String workflowDefinitionId) {
        return triggerRepository.findByWorkflowDefinitionId(workflowDefinitionId);
    }
    
    /**
     * Get all triggers by type
     */
    public List<WorkflowTrigger> getTriggersByType(WorkflowTrigger.TriggerType type) {
        return triggerRepository.findByType(type);
    }
    
    /**
     * Get all enabled triggers
     */
    public List<WorkflowTrigger> getEnabledTriggers() {
        return triggerRepository.findByEnabled(true);
    }
    
    /**
     * Enable a trigger
     */
    @Transactional
    public WorkflowTrigger enableTrigger(String triggerId) {
        log.info("Enabling trigger: id={}", triggerId);
        
        WorkflowTrigger trigger = getTrigger(triggerId);
        trigger.setEnabled(true);
        trigger.setUpdatedAt(LocalDateTime.now());
        
        return triggerRepository.save(trigger);
    }
    
    /**
     * Disable a trigger
     */
    @Transactional
    public WorkflowTrigger disableTrigger(String triggerId) {
        log.info("Disabling trigger: id={}", triggerId);
        
        WorkflowTrigger trigger = getTrigger(triggerId);
        trigger.setEnabled(false);
        trigger.setUpdatedAt(LocalDateTime.now());
        
        return triggerRepository.save(trigger);
    }
    
    /**
     * Get trigger statistics
     */
    public Map<String, Object> getTriggerStatistics(String triggerId) {
        WorkflowTrigger trigger = getTrigger(triggerId);
        
        // Use HashMap to allow null values (Map.of() doesn't allow nulls)
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("triggerId", trigger.getId());
        stats.put("name", trigger.getName());
        stats.put("type", trigger.getType());
        stats.put("enabled", trigger.getEnabled());
        stats.put("executionCount", trigger.getExecutionCount());
        stats.put("lastExecutedAt", trigger.getLastExecutedAt());
        stats.put("lastExecutionStatus", trigger.getLastExecutionStatus());
        stats.put("lastExecutionWorkflowId", trigger.getLastExecutionWorkflowId());
        stats.put("createdAt", trigger.getCreatedAt());
        
        // Add next execution time for scheduled triggers
        if (trigger.getType() == WorkflowTrigger.TriggerType.SCHEDULED && 
            trigger.getConfig() != null && 
            trigger.getConfig().getCronExpression() != null) {
            
            LocalDateTime nextExecution = scheduledTriggerService.getNextExecutionTime(
                    trigger.getConfig().getCronExpression(),
                    trigger.getConfig().getTimezone()
            );
            stats.put("nextExecutionTime", nextExecution);
        }
        
        return stats;
    }
    
    /**
     * Validate trigger creation
     */
    private void validateTriggerCreation(
            String workflowDefinitionId,
            String projectId,
            String name,
            WorkflowTrigger.TriggerType type,
            TriggerConfig config) {
        
        if (workflowDefinitionId == null || workflowDefinitionId.isEmpty()) {
            throw new IllegalArgumentException("Workflow definition ID is required");
        }
        
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID is required");
        }
        
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Trigger name is required");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Trigger type is required");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Trigger configuration is required");
        }
        
        // Type-specific validation
        switch (type) {
            case SCHEDULED:
                if (config.getCronExpression() == null || config.getCronExpression().isEmpty()) {
                    throw new IllegalArgumentException("Cron expression is required for scheduled triggers");
                }
                if (!scheduledTriggerService.validateCronExpression(config.getCronExpression())) {
                    throw new IllegalArgumentException("Invalid cron expression: " + config.getCronExpression());
                }
                break;
                
            case EVENT:
                if (config.getEventType() == null || config.getEventType().isEmpty()) {
                    throw new IllegalArgumentException("Event type is required for event triggers");
                }
                break;
                
            case WEBHOOK:
                // Webhook secret will be auto-generated if not provided
                break;
                
            case MANUAL:
                // No additional validation needed
                break;
        }
    }
    
    /**
     * Generate a secure webhook secret
     */
    private String generateWebhookSecret() {
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "");
    }
}
