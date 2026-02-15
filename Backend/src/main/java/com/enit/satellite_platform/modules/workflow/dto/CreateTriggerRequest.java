package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.TriggerConfig;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for creating a workflow trigger
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTriggerRequest {
    
    private String workflowDefinitionId;
    
    private String projectId;
    
    private String name;
    
    private String description;
    
    private WorkflowTrigger.TriggerType type;
    
    private TriggerConfig config;
    
    private Map<String, Object> defaultInputs = new HashMap<>();
}
