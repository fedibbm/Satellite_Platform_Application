package com.enit.satellite_platform.modules.workflow.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflows")
public class Workflow {
    @Id
    private String id;
    private String name;
    private String description;
    private String status; // DRAFT, ACTIVE, PAUSED, ARCHIVED
    private String projectId;
    private String currentVersion;
    private List<WorkflowVersion> versions = new ArrayList<>();
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags = new ArrayList<>();
    private boolean isTemplate;
}
