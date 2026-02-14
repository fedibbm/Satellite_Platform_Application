package com.enit.satellite_platform.modules.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating an existing workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {
    
    private String name;
    
    private String description;
    
    private String status; // DRAFT, PUBLISHED, ARCHIVED
}
