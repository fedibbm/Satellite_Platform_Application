package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowEvent;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowEventRepository;
import com.enit.satellite_platform.modules.workflow.repositories.WorkflowTriggerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for publishing workflow events
 * Events can trigger workflows based on configured triggers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventPublisher {
    
    private final WorkflowEventRepository eventRepository;
    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowExecutionService executionService;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    /**
     * Publish a workflow event
     * This will store the event and check if any triggers should execute workflows
     * 
     * @param eventType Type of event
     * @param eventSource Source of the event
     * @param projectId Project ID
     * @param userId User ID (optional)
     * @param eventData Event data
     * @return Created event
     */
    @Transactional
    public WorkflowEvent publishEvent(
            String eventType, 
            String eventSource, 
            String projectId,
            String userId,
            Map<String, Object> eventData) {
        
        log.info("Publishing workflow event: type={}, source={}, projectId={}", 
                eventType, eventSource, projectId);
        
        // Create event
        WorkflowEvent event = WorkflowEvent.builder()
                .eventType(eventType)
                .eventSource(eventSource)
                .projectId(projectId)
                .userId(userId)
                .eventData(eventData != null ? eventData : new HashMap<>())
                .timestamp(LocalDateTime.now())
                .processed(false)
                .status("PENDING")
                .triggeredWorkflows(new HashMap<>())
                .build();
        
        // Save event
        event = eventRepository.save(event);
        
        // Publish Spring application event for async processing
        applicationEventPublisher.publishEvent(new WorkflowEventTriggeredEvent(this, event));
        
        // Process event immediately (synchronous)
        processEvent(event);
        
        return event;
    }
    
    /**
     * Process a workflow event
     * Find matching triggers and execute workflows
     * 
     * @param event Event to process
     */
    @Transactional
    public void processEvent(WorkflowEvent event) {
        try {
            log.info("Processing workflow event: id={}, type={}", event.getId(), event.getEventType());
            
            event.setStatus("PROCESSING");
            eventRepository.save(event);
            
            // Find all enabled event triggers that match this event
            List<WorkflowTrigger> triggers = triggerRepository.findByEnabledAndType(
                    true, WorkflowTrigger.TriggerType.EVENT);
            
            int executedCount = 0;
            
            for (WorkflowTrigger trigger : triggers) {
                // Check if trigger matches this event
                if (matchesTrigger(event, trigger)) {
                    try {
                        log.info("Event matches trigger: triggerId={}, triggerName={}", 
                                trigger.getId(), trigger.getName());
                        
                        // Build input parameters
                        Map<String, Object> inputs = buildInputParameters(event, trigger);
                        
                        // Execute workflow
                        // Use workflowDefinitionId as workflow name for now
                        String workflowName = "workflow_" + trigger.getWorkflowDefinitionId();
                        
                        String workflowExecutionId = executionService.startWorkflow(
                                workflowName,
                                1, // version
                                inputs,
                                trigger.getWorkflowDefinitionId(),
                                trigger.getProjectId(),
                                event.getUserId()
                        );
                        
                        // Record execution
                        event.getTriggeredWorkflows().put(trigger.getId(), workflowExecutionId);
                        
                        // Update trigger statistics
                        trigger.setLastExecutedAt(LocalDateTime.now());
                        trigger.setExecutionCount(trigger.getExecutionCount() + 1);
                        trigger.setLastExecutionStatus("SUCCESS");
                        trigger.setLastExecutionWorkflowId(workflowExecutionId);
                        triggerRepository.save(trigger);
                        
                        executedCount++;
                        
                        log.info("Successfully executed workflow from event trigger: workflowId={}", 
                                workflowExecutionId);
                        
                    } catch (Exception e) {
                        log.error("Failed to execute workflow from trigger: triggerId={}", 
                                trigger.getId(), e);
                        
                        // Update trigger with error
                        trigger.setLastExecutionStatus("FAILED");
                        triggerRepository.save(trigger);
                    }
                }
            }
            
            // Mark event as processed
            event.setProcessed(true);
            event.setStatus("COMPLETED");
            event.setProcessedAt(LocalDateTime.now());
            eventRepository.save(event);
            
            log.info("Event processing completed: id={}, triggeredWorkflows={}", 
                    event.getId(), executedCount);
            
        } catch (Exception e) {
            log.error("Error processing workflow event: id={}", event.getId(), e);
            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            eventRepository.save(event);
        }
    }
    
    /**
     * Check if an event matches a trigger
     */
    private boolean matchesTrigger(WorkflowEvent event, WorkflowTrigger trigger) {
        if (trigger.getConfig() == null) {
            return false;
        }
        
        // Check event type
        String expectedEventType = trigger.getConfig().getEventType();
        if (expectedEventType == null || !expectedEventType.equals(event.getEventType())) {
            return false;
        }
        
        // Check event source (optional filter)
        String expectedSource = trigger.getConfig().getEventSource();
        if (expectedSource != null && !expectedSource.equals(event.getEventSource())) {
            return false;
        }
        
        // Check project ID
        if (trigger.getProjectId() != null && !trigger.getProjectId().equals(event.getProjectId())) {
            return false;
        }
        
        // Check event filters (optional)
        Map<String, Object> filters = trigger.getConfig().getEventFilters();
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> filter : filters.entrySet()) {
                Object eventValue = event.getEventData().get(filter.getKey());
                if (!filter.getValue().equals(eventValue)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Build input parameters for workflow execution from event data
     */
    private Map<String, Object> buildInputParameters(WorkflowEvent event, WorkflowTrigger trigger) {
        Map<String, Object> inputs = new HashMap<>();
        
        // Add default inputs from trigger
        if (trigger.getDefaultInputs() != null) {
            inputs.putAll(trigger.getDefaultInputs());
        }
        
        // Add event metadata
        inputs.put("eventId", event.getId());
        inputs.put("eventType", event.getEventType());
        inputs.put("eventSource", event.getEventSource());
        inputs.put("triggerId", trigger.getId());
        
        // Map event data to workflow inputs
        if (trigger.getConfig() != null && trigger.getConfig().getEventDataMapping() != null) {
            Map<String, String> mapping = trigger.getConfig().getEventDataMapping();
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String eventField = entry.getKey();
                String workflowParam = entry.getValue();
                Object value = event.getEventData().get(eventField);
                if (value != null) {
                    inputs.put(workflowParam, value);
                }
            }
        } else {
            // If no mapping, pass all event data as-is
            inputs.putAll(event.getEventData());
        }
        
        return inputs;
    }
    
    /**
     * Spring event for workflow event triggering
     * Can be used for async processing with @EventListener
     */
    public static class WorkflowEventTriggeredEvent extends org.springframework.context.ApplicationEvent {
        private final WorkflowEvent workflowEvent;
        
        public WorkflowEventTriggeredEvent(Object source, WorkflowEvent workflowEvent) {
            super(source);
            this.workflowEvent = workflowEvent;
        }
        
        public WorkflowEvent getWorkflowEvent() {
            return workflowEvent;
        }
    }
}
