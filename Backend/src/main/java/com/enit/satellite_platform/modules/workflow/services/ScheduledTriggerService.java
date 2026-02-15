package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling scheduled workflow triggers
 * Uses cron expressions to execute workflows at specific times
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTriggerService {
    
    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowExecutionService executionService;
    
    // Cache of last execution times to prevent duplicate executions
    private final Map<String, LocalDateTime> lastExecutionTimes = new HashMap<>();
    
    /**
     * Check and execute scheduled triggers every minute
     * This is the scheduler that checks all cron-based triggers
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at 0 seconds
    public void checkScheduledTriggers() {
        try {
            log.debug("Checking scheduled workflow triggers...");
            
            // Find all enabled scheduled triggers
            List<WorkflowTrigger> scheduledTriggers = triggerRepository
                    .findByEnabledAndType(true, WorkflowTrigger.TriggerType.SCHEDULED);
            
            if (scheduledTriggers.isEmpty()) {
                log.debug("No enabled scheduled triggers found");
                return;
            }
            
            log.info("Found {} enabled scheduled triggers", scheduledTriggers.size());
            
            LocalDateTime now = LocalDateTime.now();
            
            for (WorkflowTrigger trigger : scheduledTriggers) {
                try {
                    if (shouldExecute(trigger, now)) {
                        executeScheduledWorkflow(trigger);
                    }
                } catch (Exception e) {
                    log.error("Error processing scheduled trigger: id={}, name={}", 
                            trigger.getId(), trigger.getName(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in scheduled trigger check", e);
        }
    }
    
    /**
     * Check if a scheduled trigger should execute now
     */
    private boolean shouldExecute(WorkflowTrigger trigger, LocalDateTime now) {
        if (trigger.getConfig() == null || trigger.getConfig().getCronExpression() == null) {
            log.warn("Scheduled trigger missing cron expression: id={}", trigger.getId());
            return false;
        }
        
        try {
            // Parse cron expression
            String cronExpr = trigger.getConfig().getCronExpression();
            CronExpression cron = CronExpression.parse(cronExpr);
            
            // Get timezone
            ZoneId timezone = ZoneId.of(
                    trigger.getConfig().getTimezone() != null 
                            ? trigger.getConfig().getTimezone() 
                            : "UTC"
            );
            
            ZonedDateTime zonedNow = now.atZone(timezone);
            
            // Check if we should execute based on cron
            // Get the last execution time
            LocalDateTime lastExecution = lastExecutionTimes.get(trigger.getId());
            if (lastExecution == null) {
                lastExecution = trigger.getLastExecutedAt();
            }
            
            // If never executed, check if cron matches current time
            if (lastExecution == null) {
                ZonedDateTime nextExecution = cron.next(zonedNow.minusMinutes(1));
                if (nextExecution != null && 
                    nextExecution.toLocalDateTime().isBefore(now.plusSeconds(30))) {
                    return checkAdditionalConstraints(trigger, now);
                }
                return false;
            }
            
            // Check if it's time for next execution
            ZonedDateTime zonedLastExecution = lastExecution.atZone(timezone);
            ZonedDateTime nextExecution = cron.next(zonedLastExecution);
            
            if (nextExecution != null && 
                !nextExecution.toLocalDateTime().isAfter(now)) {
                
                // Check additional constraints
                return checkAdditionalConstraints(trigger, now);
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error evaluating cron expression for trigger: id={}, cron={}", 
                    trigger.getId(), trigger.getConfig().getCronExpression(), e);
            return false;
        }
    }
    
    /**
     * Check additional scheduling constraints
     */
    private boolean checkAdditionalConstraints(WorkflowTrigger trigger, LocalDateTime now) {
        // Check start date
        if (trigger.getConfig().getStartDate() != null) {
            try {
                LocalDateTime startDate = LocalDateTime.parse(trigger.getConfig().getStartDate());
                if (now.isBefore(startDate)) {
                    log.debug("Trigger not yet started: id={}, startDate={}", 
                            trigger.getId(), startDate);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Invalid start date for trigger: id={}", trigger.getId(), e);
            }
        }
        
        // Check end date
        if (trigger.getConfig().getEndDate() != null) {
            try {
                LocalDateTime endDate = LocalDateTime.parse(trigger.getConfig().getEndDate());
                if (now.isAfter(endDate)) {
                    log.debug("Trigger expired: id={}, endDate={}", trigger.getId(), endDate);
                    // Disable the trigger
                    trigger.setEnabled(false);
                    triggerRepository.save(trigger);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Invalid end date for trigger: id={}", trigger.getId(), e);
            }
        }
        
        // Check max executions
        if (trigger.getConfig().getMaxExecutions() != null) {
            if (trigger.getExecutionCount() >= trigger.getConfig().getMaxExecutions()) {
                log.info("Trigger reached max executions: id={}, count={}", 
                        trigger.getId(), trigger.getExecutionCount());
                // Disable the trigger
                trigger.setEnabled(false);
                triggerRepository.save(trigger);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Execute a scheduled workflow
     */
    private void executeScheduledWorkflow(WorkflowTrigger trigger) {
        try {
            log.info("Executing scheduled workflow: triggerId={}, triggerName={}, workflowDefId={}", 
                    trigger.getId(), trigger.getName(), trigger.getWorkflowDefinitionId());
            
            // Build input parameters
            Map<String, Object> inputs = new HashMap<>();
            if (trigger.getDefaultInputs() != null) {
                inputs.putAll(trigger.getDefaultInputs());
            }
            
            // Add trigger metadata
            inputs.put("triggerId", trigger.getId());
            inputs.put("triggerType", "SCHEDULED");
            inputs.put("executionTime", LocalDateTime.now().toString());
            
            // Execute workflow
            // First, get the workflow definition to get the workflow name
            // For now, use workflowDefinitionId as workflowName
            String workflowName = "workflow_" + trigger.getWorkflowDefinitionId();
            
            String workflowExecutionId = executionService.startWorkflow(
                    workflowName,
                    1, // version
                    inputs,
                    trigger.getWorkflowDefinitionId(),
                    trigger.getProjectId(),
                    trigger.getCreatedBy()
            );
            
            // Update trigger statistics
            LocalDateTime now = LocalDateTime.now();
            trigger.setLastExecutedAt(now);
            trigger.setExecutionCount(trigger.getExecutionCount() + 1);
            trigger.setLastExecutionStatus("SUCCESS");
            trigger.setLastExecutionWorkflowId(workflowExecutionId);
            trigger.setUpdatedAt(now);
            triggerRepository.save(trigger);
            
            // Update cache
            lastExecutionTimes.put(trigger.getId(), now);
            
            log.info("Successfully executed scheduled workflow: triggerId={}, workflowId={}, executionCount={}", 
                    trigger.getId(), workflowExecutionId, trigger.getExecutionCount());
            
        } catch (Exception e) {
            log.error("Failed to execute scheduled workflow: triggerId={}", trigger.getId(), e);
            
            // Update trigger with error
            trigger.setLastExecutionStatus("FAILED");
            trigger.setUpdatedAt(LocalDateTime.now());
            triggerRepository.save(trigger);
        }
    }
    
    /**
     * Validate a cron expression
     * 
     * @param cronExpression Cron expression to validate
     * @return true if valid
     */
    public boolean validateCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            log.warn("Invalid cron expression: {}", cronExpression, e);
            return false;
        }
    }
    
    /**
     * Get next execution time for a cron expression
     * 
     * @param cronExpression Cron expression
     * @param timezone Timezone
     * @return Next execution time
     */
    public LocalDateTime getNextExecutionTime(String cronExpression, String timezone) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            ZoneId zone = ZoneId.of(timezone != null ? timezone : "UTC");
            ZonedDateTime next = cron.next(ZonedDateTime.now(zone));
            return next != null ? next.toLocalDateTime() : null;
        } catch (Exception e) {
            log.error("Error calculating next execution time", e);
            return null;
        }
    }
}
