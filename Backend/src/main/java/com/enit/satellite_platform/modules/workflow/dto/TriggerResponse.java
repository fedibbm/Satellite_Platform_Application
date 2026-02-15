package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.TriggerConfig;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowTrigger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for workflow trigger response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResponse {
    
    private String id;
    
    private String name;
    
    private String description;
    
    private String workflowDefinitionId;
    
    private String projectId;
    
    private WorkflowTrigger.TriggerType type;
    
    private TriggerConfig config;
    
    private Map<String, Object> defaultInputs;
    
    private Boolean enabled;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastExecutedAt;
    
    private Long executionCount;
    
    private String lastExecutionStatus;
    
    private String lastExecutionWorkflowId;
    
    // Additional computed fields
    private String webhookUrl; // For webhook triggers
    
    private LocalDateTime nextExecutionTime; // For scheduled triggers
    
    /**
     * Convert entity to DTO
     */
    public static TriggerResponse fromEntity(WorkflowTrigger trigger) {
        TriggerResponse response = TriggerResponse.builder()
                .id(trigger.getId())
                .name(trigger.getName())
                .description(trigger.getDescription())
                .workflowDefinitionId(trigger.getWorkflowDefinitionId())
                .projectId(trigger.getProjectId())
                .type(trigger.getType())
                .config(trigger.getConfig())
                .defaultInputs(trigger.getDefaultInputs())
                .enabled(trigger.getEnabled())
                .createdBy(trigger.getCreatedBy())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .lastExecutedAt(trigger.getLastExecutedAt())
                .executionCount(trigger.getExecutionCount())
                .lastExecutionStatus(trigger.getLastExecutionStatus())
                .lastExecutionWorkflowId(trigger.getLastExecutionWorkflowId())
                .build();
        
        // Add webhook URL for webhook triggers
        if (trigger.getType() == WorkflowTrigger.TriggerType.WEBHOOK) {
            response.setWebhookUrl("/api/webhooks/trigger/" + trigger.getId());
        }
        
        return response;
    }
}
