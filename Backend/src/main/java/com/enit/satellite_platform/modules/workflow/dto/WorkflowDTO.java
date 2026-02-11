package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowDTO {
    private String id;
    private String name;
    private String description;
    private WorkflowStatus status;
    private String projectId;
    private String currentVersion;
    private List<WorkflowVersion> versions;
    private List<WorkflowExecutionDTO> executions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<String> tags;
    private Boolean isTemplate;
}
