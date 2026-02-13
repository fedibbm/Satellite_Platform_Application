package com.enit.satellite_platform.modules.workflow.dto;

import com.enit.satellite_platform.modules.workflow.entities.WorkflowExecution;
import com.enit.satellite_platform.modules.workflow.entities.WorkflowVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    private String id;
    private String name;
    private String description;
    private String status;
    private String projectId;
    private String currentVersion;
    private List<WorkflowVersion> versions = new ArrayList<>();
    private List<WorkflowExecution> executions = new ArrayList<>();
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags = new ArrayList<>();
    private boolean isTemplate;
}
