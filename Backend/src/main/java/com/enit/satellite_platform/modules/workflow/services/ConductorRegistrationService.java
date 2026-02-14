package com.enit.satellite_platform.modules.workflow.services;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowDefinition;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import io.orkes.conductor.client.http.OrkesMetadataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for registering workflows with Conductor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConductorRegistrationService {
    
    private final OrkesMetadataClient metadataClient;
    private final ConductorWorkflowBuilder workflowBuilder;
    
    /**
     * Register workflow definition with Conductor
     */
    public void registerWorkflow(WorkflowDefinition workflow) {
        log.info("Registering workflow with Conductor: {}", workflow.getName());
        
        try {
            // Build Conductor workflow definition
            WorkflowDef workflowDef = workflowBuilder.buildWorkflowDef(workflow);
            
            // Validate before registration
            if (!workflowBuilder.validateWorkflowDef(workflowDef)) {
                throw new RuntimeException("Workflow validation failed");
            }
            
            // Register with Conductor
            metadataClient.registerWorkflowDef(workflowDef);
            
            log.info("Successfully registered workflow: {} (version {})", 
                    workflowDef.getName(), workflowDef.getVersion());
            
        } catch (Exception e) {
            log.error("Failed to register workflow with Conductor: {}", workflow.getName(), e);
            throw new RuntimeException("Failed to register workflow with Conductor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update existing workflow definition in Conductor
     */
    public void updateWorkflow(WorkflowDefinition workflow) {
        log.info("Updating workflow in Conductor: {}", workflow.getName());
        
        try {
            // Build Conductor workflow definition
            WorkflowDef workflowDef = workflowBuilder.buildWorkflowDef(workflow);
            
            // Validate before update
            if (!workflowBuilder.validateWorkflowDef(workflowDef)) {
                throw new RuntimeException("Workflow validation failed");
            }
            
            // Update in Conductor (will create new version)
            metadataClient.updateWorkflowDefs(java.util.Arrays.asList(workflowDef));
            
            log.info("Successfully updated workflow: {} (version {})", 
                    workflowDef.getName(), workflowDef.getVersion());
            
        } catch (Exception e) {
            log.error("Failed to update workflow in Conductor: {}", workflow.getName(), e);
            throw new RuntimeException("Failed to update workflow in Conductor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Unregister workflow from Conductor
     */
    public void unregisterWorkflow(String workflowName, Integer version) {
        log.info("Unregistering workflow from Conductor: {} version {}", workflowName, version);
        
        try {
            metadataClient.unregisterWorkflowDef(workflowName, version);
            log.info("Successfully unregistered workflow: {}", workflowName);
        } catch (Exception e) {
            log.error("Failed to unregister workflow from Conductor: {}", workflowName, e);
            throw new RuntimeException("Failed to unregister workflow from Conductor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if workflow is registered in Conductor
     */
    public boolean isWorkflowRegistered(String workflowName) {
        try {
            WorkflowDef workflowDef = metadataClient.getWorkflowDef(workflowName, null);
            return workflowDef != null;
        } catch (Exception e) {
            log.debug("Workflow not found in Conductor: {}", workflowName);
            return false;
        }
    }
    
    /**
     * Get workflow definition from Conductor
     */
    public WorkflowDef getWorkflowDef(String workflowName, Integer version) {
        try {
            return metadataClient.getWorkflowDef(workflowName, version);
        } catch (Exception e) {
            log.error("Failed to get workflow from Conductor: {}", workflowName, e);
            return null;
        }
    }
}
