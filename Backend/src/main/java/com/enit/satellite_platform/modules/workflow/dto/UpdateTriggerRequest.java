package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.TriggerConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for updating a workflow trigger
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTriggerRequest {
    
    private String name;
    
    private String description;
    
    private TriggerConfig config;
    
    private Map<String, Object> defaultInputs;
    
    private Boolean enabled;
}
